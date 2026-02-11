const { Client } = require('@stomp/stompjs');
const WebSocket = require('ws');

const TOKEN = "eyJhbGciOiJFUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJydWJlbkFkbWluIiwidXNlcl9pZCI6IjQ2ZDc2YWYwLWVkNDctNGY2Mi04OTFhLTcyOGRlYWQ0NmU4NSIsInJvbGUiOiJBRE1JTiIsInNjb3BlIjoib3JkZXI6dmlldyBvcmRlcjp2aWV3X293biBvcmRlcjpjcmVhdGUgb3JkZXI6dXBkYXRlIG9yZGVyOnZpZXdfbm9waWNrZWQgZmxlZXQ6Y3JlYXRlIGZsZWV0OnVwZGF0ZSBmbGVldDp2aWV3Iiwiem9uZV9pZCI6bnVsbCwiZmxlZXRfdHlwZSI6bnVsbCwiaWF0IjoxNzcwNzg2ODI1LCJleHAiOjE3NzA3OTA0MjV9.zAzEMWXcNtcgosxezrJUIZtoLivLuEOIkoVd0LxoyP7HDH-TuVXptM5fRtAsuqGGguD1K8xS2G4rDwz7Eh9RYw";

const client = new Client({

    webSocketFactory: () => {
        return new WebSocket(
            'ws://localhost:8082/ws-notifications',
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
