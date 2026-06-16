import axios from "axios";

const client = axios.create({
    baseURL: 'http://localhost:8080/api',
    headers: {
        "Content-Type": 'application/json',
    },
});

client.interceptors.response.use(
    (response) => response.data.data,
    (error) => {
        const message = error.response?.data?.message || "Something went wrong";
        console.error('API error:', message);
        return Promise.reject(message);
    }
);

export default client;