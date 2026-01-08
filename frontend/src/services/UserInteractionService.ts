import axios from 'axios';

const API_URL = 'http://localhost:8081/api/v1';

export interface UserBookInteraction {
    id: number;
    bookId: number;
    bookTitle: string;
    bookAuthor: string;
    addedAt: string;
}

const getAuthHeader = () => {
    const userStr = localStorage.getItem('user');
    if (userStr) {
        const user = JSON.parse(userStr);
        return { Authorization: `Bearer ${user.token}` };
    }
    return {};
};

// Favorites
export const addFavorite = async (bookId: number): Promise<void> => {
    await axios.post(`${API_URL}/favorites/${bookId}`, {}, { headers: getAuthHeader() });
};

export const removeFavorite = async (bookId: number): Promise<void> => {
    await axios.delete(`${API_URL}/favorites/${bookId}`, { headers: getAuthHeader() });
};

export const getFavorites = async (): Promise<UserBookInteraction[]> => {
    const response = await axios.get(`${API_URL}/favorites`, { headers: getAuthHeader() });
    return response.data;
};

// Reading List
export const addToReadingList = async (bookId: number): Promise<void> => {
    await axios.post(`${API_URL}/reading-list/${bookId}`, {}, { headers: getAuthHeader() });
};

export const removeFromReadingList = async (bookId: number): Promise<void> => {
    await axios.delete(`${API_URL}/reading-list/${bookId}`, { headers: getAuthHeader() });
};

export const getReadingList = async (): Promise<UserBookInteraction[]> => {
    const response = await axios.get(`${API_URL}/reading-list`, { headers: getAuthHeader() });
    return response.data;
};
