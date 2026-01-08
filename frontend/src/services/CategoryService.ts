import api from './api';

export interface Category {
    id: number;
    name: string;
    description: string;
    status: 'ACTIVE' | 'INACTIVE';
}

export const CategoryService = {
    getAllCategories: async () => {
        const response = await api.get('/categories');
        return response.data;
    },

    getCategoryById: async (id: number) => {
        const response = await api.get(`/categories/${id}`);
        return response.data;
    },

    createCategory: async (category: Omit<Category, 'id'>) => {
        const response = await api.post('/categories', category);
        return response.data;
    },

    updateCategory: async (id: number, category: Omit<Category, 'id'>) => {
        const response = await api.put(`/categories/${id}`, category);
        return response.data;
    },

    deleteCategory: async (id: number) => {
        await api.delete(`/categories/${id}`);
    }
};
