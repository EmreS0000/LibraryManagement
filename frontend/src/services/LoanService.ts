import api from './api';

export const LoanService = {
    getAll: (page = 0, size = 20) => {
        return api.get(`/loans?page=${page}&size=${size}`);
    },

    getMyLoans: (_userId: number | string) => {
        // If backend lacks specific endpoint, fetch all and filter client side
        // Ideally backend should have GET /loans/member/{id}
        // Assuming we rely on getAll for now or the filter method implemented in Component
        return api.get('/loans');
    },

    borrowBook: (bookId: number, memberId: number) => {
        return api.post('/loans/borrow', { bookId, memberId });
    },

    returnBook: (loanId: number) => {
        return api.post(`/loans/${loanId}/return`);
    }
};
