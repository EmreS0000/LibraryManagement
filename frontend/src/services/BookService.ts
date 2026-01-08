import api from './api';

export const BookService = {
    getAll: (page = 0, size = 10) => {
        return api.get(`/books?page=${page}&size=${size}`);
    },

    search: (query: string, page = 0, size = 10) => {
        return api.get(`/books/search?query=${query}&page=${page}&size=${size}`);
    },

    getById: (id: number | string) => {
        return api.get(`/books/${id}`);
    },

    // Admin functions can be added here
    create: (data: any) => api.post('/books', data),
    update: (id: number, data: any) => api.put(`/books/${id}`, data),
    delete: (id: number) => api.delete(`/books/${id}`)
};
