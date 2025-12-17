package com.espe.edu.ec.order_ms.services;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.espe.edu.ec.order_ms.dtos.AssignDriverRequest;
import com.espe.edu.ec.order_ms.dtos.DeliveryOrderPatchRequest;
import com.espe.edu.ec.order_ms.dtos.OrderPatchRequest;
import com.espe.edu.ec.order_ms.dtos.OrderRequest;
import com.espe.edu.ec.order_ms.dtos.OrderResponse;
import com.espe.edu.ec.order_ms.mappers.OrderMapper;
import com.espe.edu.ec.order_ms.model_enums.OrderStatus;
import com.espe.edu.ec.order_ms.model_enums.VehicleType;
import com.espe.edu.ec.order_ms.models.Address;
import com.espe.edu.ec.order_ms.models.Order;
import com.espe.edu.ec.order_ms.models.Tariff;
import com.espe.edu.ec.order_ms.repositories.OrderRepository;
import com.espe.edu.ec.order_ms.repositories.TariffRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final TariffRepository tariffRepository;
    // private final DriverEventProducer driverEventProducer;

    @Override
    @Transactional
    public OrderResponse createOrder(OrderRequest orderRequest) {

        log.info("Iniciando creación de pedido para cliente: {}", orderRequest.getCustomerId());

        Order order = OrderMapper.orderRequestToEntity(orderRequest);
        calculateOrderValues(order, orderRequest.getVehicleType());
        
        Order newOrder = orderRepository.save(order);
        return OrderMapper.entityToOrderResponse(newOrder);

    }

    @Override
    public OrderResponse getOrder(UUID id) {

        Order foundOrder = orderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pedido no encontrado: " + id));
        return OrderMapper.entityToOrderResponse(foundOrder);

    }

    @Override
    public List<OrderResponse> getOrders() {
        return orderRepository.findAll()
            .stream()
            .map(OrderMapper::entityToOrderResponse) 
            .toList();
    }

    @Override
    public boolean orderExists(UUID id){
        return getOrder(id) == null ? false : true;
    }

    @Override
    @Transactional
    public OrderResponse patchOrder(UUID id, OrderPatchRequest orderPatchRequest) {
        
        Order foundOrder = orderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pedido no encontrado: " + id));

        if (!OrderStatus.CREATED.equals(foundOrder.getStatus()))
            throw new IllegalStateException("Solo se pueden editar pedidos en estado CREATED");
        
        if(orderPatchRequest instanceof DeliveryOrderPatchRequest deliveryOrderPatchRequest){

            foundOrder.getDeliveryAddress().setInstructions(orderPatchRequest.getInstructions());

            if (deliveryOrderPatchRequest.getNewCoordinates() != null) 
                foundOrder.getDeliveryAddress().setCoordinates(deliveryOrderPatchRequest.getNewCoordinates());
        }

        // TODO: Implement Pickup Patch Request

        Order updatedOrder = orderRepository.save(foundOrder);
        return OrderMapper.entityToOrderResponse(updatedOrder);

    }

    @Override
    @Transactional
    // TODO: Integrate routing logic
    public void cancelOrder(UUID id) {
        
        Order foundOrder = orderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pedido no encontrado: " + id));

        if (OrderStatus.CREATED.equals(foundOrder.getStatus()) || OrderStatus.PICKED_UP.equals(foundOrder.getStatus())) {
            log.info("Cancelación local inmediata para pedido: {}", id);
            foundOrder.setStatus(OrderStatus.CANCELLED);
            orderRepository.save(foundOrder);
        }else{
            throw new IllegalStateException("Una orden solo puede ser cancelada si fue creada o si fue recogida.");
        }
    }

    @Override
    public List<OrderResponse> getOrdersByCustomer(UUID customerId) {
        return orderRepository.findByCustomerId(customerId)
            .stream()
            .map(OrderMapper::entityToOrderResponse)
            .toList();
    }

    @Override
    @Transactional
    public OrderResponse assignDriverAndVehicle(UUID orderId, AssignDriverRequest request) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Pedido no encontrado: " + orderId));

        
        if (OrderStatus.CANCELLED.equals(order.getStatus()))
           throw new IllegalStateException("No se puede asignar recursos a una orden cancelada.");
        

        order.setDriverId(request.getDriverId());
        order.setVehicleId(request.getVehicleId());
        
        // Nota: No cambiamos el estado automáticamente a menos que sea un requerimiento.
        // Si quisieras que pase a "IN_ROUTE" o similar, descomenta abajo:
        // order.setStatus(OrderStatus.IN_ROUTE); 

        Order updatedOrder = orderRepository.save(order);
        return OrderMapper.entityToOrderResponse(updatedOrder);
    }

    /**
     * Recalcula los valores del pedido basándose en las direcciones actuales.
     * Valida que la distancia esté dentro de cobertura y actualiza:
     * - Distancia
     * - Trip Fee
     * - Service Fee
     * - Total Amount
     */
    private void calculateOrderValues(Order order, VehicleType vehicleType) {
        
        double distance = OrderUtils.calculateDistance(order.getPickupAddress(), order.getDeliveryAddress());
        BigDecimal distanceBd = BigDecimal.valueOf(distance).setScale(2, RoundingMode.HALF_UP);
        
        if (distance > OrderUtils.MAX_COVERAGE) 
            throw new IllegalArgumentException("La distancia excede la cobertura operativa (Max 50km).");
        
        Tariff tariff = tariffRepository.findByVehicleType(vehicleType)
                .orElseThrow(() -> new IllegalArgumentException("No hay tarifa configurada para este vehículo"));

        BigDecimal tripFee = OrderUtils.calculateTripFee(tariff, distanceBd);
        BigDecimal serviceFee = tariff.getBaseCost();
        BigDecimal totalAmount = tripFee.add(serviceFee);

        order.setDistanceKm(distanceBd);
        order.setTripFee(tripFee);
        order.setServiceFee(serviceFee);
        order.setTotalAmount(totalAmount);
        order.setStatus(OrderStatus.CREATED);
    }

    // Clase utilitaria interna (privada y final según buenas prácticas anteriores)
    private final class OrderUtils {

        private OrderUtils(){
            throw new UnsupportedOperationException("Clase utilitaria");
        }

        private static final int EARTH_RADIUS_KM = 6371;
        public static final int MAX_COVERAGE = 50;

        public static double calculateDistance(Address addr1, Address addr2){

            if (addr1 == null || addr2 == null) 
                throw new IllegalArgumentException("Las direcciones de origen y destino no pueden ser nulas.");
            
            if (addr1.getCoordinates() == null || addr2.getCoordinates() == null)
                throw new IllegalArgumentException("Las coordenadas de las direcciones no pueden ser nulas.");
            
            double lat1 = addr1.getCoordinates().getLatitude();
            double lon1 = addr1.getCoordinates().getLongitude();
            double lat2 = addr2.getCoordinates().getLatitude();
            double lon2 = addr2.getCoordinates().getLongitude();

            if (!isValidCoordinate(lat1, lon1) || !isValidCoordinate(lat2, lon2))
                throw new IllegalArgumentException("Las coordenadas proporcionadas son inválidas.");

            double dLat = Math.toRadians(lat2 - lat1);
            double dLon = Math.toRadians(lon2 - lon1);

            double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                    Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                    Math.sin(dLon / 2) * Math.sin(dLon / 2);

            double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

            return EARTH_RADIUS_KM * c;

        }

        public static BigDecimal calculateTripFee(Tariff tariff, BigDecimal distance) {

            if (tariff == null)
                throw new IllegalArgumentException("La tarifa no puede ser nula.");
            
            if (distance == null)
                throw new IllegalArgumentException("La distancia no puede ser nula.");
            
            if (distance.compareTo(BigDecimal.ZERO) < 0)
                throw new IllegalArgumentException("La distancia no puede ser negativa.");

            if (distance.compareTo(BigDecimal.valueOf(tariff.getMinDistanceKm())) < 0) {
                return tariff.getBaseCost();
            }
            return distance.multiply(tariff.getCostPerKm());

        }

        private static boolean isValidCoordinate(double latitude, double longitude) {
            return latitude >= -90 && latitude <= 90 && longitude >= -180 && longitude <= 180;
        }
    }
}