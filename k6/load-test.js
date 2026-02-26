import http from 'k6/http';
import { sleep, check } from 'k6';

export const options = {
    stages: [
        { duration: '30s', target: 50 },
        { duration: '1m', target: 100 },
        { duration: '30s', target: 0 },
    ],
};

export default function () {
    // ВАЖНО: имя хоста должно совпадать с именем сервиса в compose.yaml
    const baseUrl = 'http://recommendation-service:8026';
    const userId = Math.floor(Math.random() * 1000);

    const res = http.get(`${baseUrl}/api/recommendations/user_${userId}`);

    check(res, {
        'status is 200': (r) => r.status === 200,
    });

    sleep(0.5);
}