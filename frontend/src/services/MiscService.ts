import api from './api';

export const FineService = {
    // Backend doesn't have a standalone /fines endpoint exposed yet based on Controller list
    // Fines are part of the detailed Loan object.
    // This service is a placeholder for future extension.
    payFine: (loanId: number) => {
        // Hypothetical endpoint
        return api.post(`/loans/${loanId}/pay-fine`);
    }
};

export const ReservationService = {
    getAll: () => api.get('/reservations'),

    create: (bookId: number, memberId: number) => {
        return api.post('/reservations', {
            bookId,
            memberId,
            status: 'PENDING'
        });
    },

    cancel: (id: number) => {
        return api.delete(`/reservations/${id}`);
    }
};
