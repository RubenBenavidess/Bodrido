const { Client } = require('@stomp/stompjs');
const WebSocket = require('ws');

const TOKEN = "eyJhbGciOiJFUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJzb3RvQ2xpZW50IiwidXNlcl9pZCI6ImIwNGUwNGVlLWQ5M2QtNGVlMy05ZjY3LWQzOGU2ZjA3NmQwZCIsInJvbGUiOiJDTElFTlQiLCJzY29wZSI6Im9yZGVyOmNyZWF0ZSBvcmRlcjp2aWV3X293biIsInpvbmVfaWQiOm51bGwsImZsZWV0X3R5cGUiOm51bGwsImlhdCI6MTc3MDgyMzQ2MCwiZXhwIjoxNzcwODI3MDYwfQ.AX6slE74vGlXhydN21Lpg_X0T5W1YZlN5tr8Vdo5kjICKRiw16nFYMMqYh5-MhwUR7mHhYp6foaqsqn0wDMKIQ";

const client = new Client({

    webSocketFactory: () => {
        return new WebSocket(
            'ws://10.101.77.106:8082/ws-notifications',
            {
                headers: {
                    Authorization: `Bearer ${TOKEN}`
                }
            }
        );
    },

    connectHeaders: {
        Authorization: `Bearer ${TOKEN}`
    },

    debug: (str) => console.log(str),

    reconnectDelay: 5000,
});

client.onConnect = (frame) => {
    console.log('✅ Conectado');

    client.subscribe('/topic/notifications', (message) => {
        console.log('📩 Notificación:', JSON.parse(message.body));
    });
};

client.onStompError = (frame) => {
    console.error('❌ STOMP Error:', frame.headers['message']);
};

client.activate();
