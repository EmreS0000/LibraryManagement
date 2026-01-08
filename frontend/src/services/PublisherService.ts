import api from './api';

export interface Publisher {
    id: number;
    name: string;
    country: string;
    foundedYear: number;
    createdAt?: string;
    updatedAt?: string;
}

export const PublisherService = {
    getAllPublishers: async () => {
        const response = await api.get<Publisher[]>('/publishers');
        return response.data;
    },

    getPublisherById: async (id: number) => {
        const response = await api.get<Publisher>(`/publishers/${id}`);
        return response.data;
    },

    createPublisher: async (publisher: Omit<Publisher, 'id' | 'createdAt' | 'updatedAt'>) => {
        const response = await api.post<Publisher>('/publishers', publisher);
        return response.data;
    },

    updatePublisher: async (id: number, publisher: Omit<Publisher, 'id' | 'createdAt' | 'updatedAt'>) => {
        const response = await api.put<Publisher>(`/publishers/${id}`, publisher);
        return response.data;
    },

    deletePublisher: async (id: number) => {
        await api.delete(`/publishers/${id}`);
    }
};
