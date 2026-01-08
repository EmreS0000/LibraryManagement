import axios from 'axios';

const API_URL = 'http://localhost:8081/api/v1/reporting';

export interface CategoryReport {
    categoryName: string;
    loanCount: number;
}

export interface MemberActivity {
    memberId: number;
    memberName: string;
    loanCount: number;
}

export interface BookStatusReport {
    status: string;
    count: number;
}

const getAuthHeader = () => {
    const userStr = localStorage.getItem('user');
    if (userStr) {
        const user = JSON.parse(userStr);
        return { Authorization: `Bearer ${user.token}` };
    }
    return {};
};

export const getMostReadCategories = async (limit: number = 10): Promise<CategoryReport[]> => {
    const response = await axios.get(`${API_URL}/categories/most-read?limit=${limit}`, { headers: getAuthHeader() });
    return response.data;
};

export const getMostActiveMembers = async (limit: number = 10): Promise<MemberActivity[]> => {
    const response = await axios.get(`${API_URL}/members/most-active?limit=${limit}`, { headers: getAuthHeader() });
    return response.data;
};

export const getBookStatusDistribution = async (): Promise<BookStatusReport[]> => {
    const response = await axios.get(`${API_URL}/books/status-distribution`, { headers: getAuthHeader() });
    return response.data;
};
