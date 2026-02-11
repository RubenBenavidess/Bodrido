const { Client } = require('@stomp/stompjs');
const WebSocket = require('ws');

const TOKEN = "eyJhbGciOiJFUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJqb2FuQ2xpZW50IiwidXNlcl9pZCI6ImE3YTYwMzVhLTg5NjYtNDZmNC1iNzdjLTI1NjY2ZWY4MTVmOSIsInJvbGUiOiJDTElFTlQiLCJzY29wZSI6Im9yZGVyOmNyZWF0ZSBvcmRlcjp2aWV3X293biIsInpvbmVfaWQiOm51bGwsImZsZWV0X3R5cGUiOm51bGwsImlhdCI6MTc3MDgyMDI4NSwiZXhwIjoxNzcwODIzODg1fQ.zA3NX3O_dvTTmbraw42G9W3ejnFFzg2P1ADsTZzB987IetCffTW3Xkjb9mvKrRks3Hu3ELoKqnxA-fuqBvmYqQ";

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
