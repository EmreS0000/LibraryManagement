import axios from 'axios';

const API_URL = 'http://localhost:8081/api/v1/ratings';

export interface BookRating {
    id: number;
    bookId: number;
    memberId: number;
    memberName: string;
    score: number;
    comment: string;
    createdAt: string;
}

export interface BookRatingRequest {
    bookId: number;
    score: number;
    comment: string;
}

const getAuthHeader = () => {
    const userStr = localStorage.getItem('user');
    if (userStr) {
        const user = JSON.parse(userStr);
        return { Authorization: `Bearer ${user.token}` };
    }
    return {};
};

export const getRatingsForBook = async (bookId: number): Promise<BookRating[]> => {
    const response = await axios.get(`${API_URL}/book/${bookId}`);
    return response.data;
};

export const getAverageRating = async (bookId: number): Promise<number> => {
    const response = await axios.get(`${API_URL}/average/${bookId}`);
    return response.data;
};

export const addRating = async (rating: BookRatingRequest): Promise<BookRating> => {
    const response = await axios.post(API_URL, rating, { headers: getAuthHeader() });
    return response.data;
};

export const deleteRating = async (ratingId: number): Promise<void> => {
    await axios.delete(`${API_URL}/${ratingId}`, { headers: getAuthHeader() });
};
