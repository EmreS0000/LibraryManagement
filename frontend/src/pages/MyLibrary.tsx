import { useEffect, useState } from 'react';
import {
    Box, Typography, Paper, Table, TableBody, TableCell,
    TableContainer, TableHead, TableRow, Button, Chip, Alert,
    CircularProgress, Tabs, Tab
} from '@mui/material';
import api from '../services/api';
import { AuthService } from '../services/AuthService';

interface Loan {
    id: number;
    bookId?: number;
    bookTitle?: string;
    memberId?: number;
    loanDate: string;
    dueDate?: string;
    returnDate: string | null;
    status: 'ACTIVE' | 'RETURNED' | 'OVERDUE';
    fineAmount?: number;
}

interface Reservation {
    id: number;
    bookId: number;
    memberId: number;
    requestDate: string;
    status: 'PENDING' | 'FULFILLED' | 'CANCELLED';
}

const MyLibrary: React.FC = () => {
    const [tab, setTab] = useState(0);
    const [loans, setLoans] = useState<Loan[]>([]);
    const [reservations, setReservations] = useState<Reservation[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');

    useEffect(() => {
        fetchData();
    }, []);

    const fetchData = async () => {
        setLoading(true);
        try {
            const user = AuthService.getCurrentUser();
            if (!user) return; // Should likely redirect, but RequireAuth handles page access

            // Fetch Loans for this member
            const loansRes = await api.get(`/loans/member/${user.id}`);
            const normalizeLoan = (l: any): Loan => ({
                ...l,
                memberId: l.member?.id,
                bookId: l.bookCopy?.book?.id,
                bookTitle: l.bookCopy?.book?.title,
                dueDate: l.dueDate,
            });

            const userLoans = (loansRes.data.content || loansRes.data || [])
                .map(normalizeLoan)
                .filter((l: Loan) => l.memberId === user.id);
            setLoans(userLoans);

            // Fetch Reservations (Using /reservations and filtering)
            try {
                const resRes = await api.get('/reservations');
                const userRes = (resRes.data.content || resRes.data || [])
                    .filter((r: Reservation) => r.memberId === user.id);
                setReservations(userRes);
            } catch (e) {
                console.warn('Reservations fetch failed', e);
            }

        } catch (err) {
            setError('Failed to load library data.');
        } finally {
            setLoading(false);
        }
    };

    const handleReturnBook = async (loanId: number) => {
        if (!window.confirm('Are you sure you want to return this book?')) return;
        try {
            await api.post(`/loans/${loanId}/return`);
            alert('Book returned successfully!');
            // Notify other pages (BookList/BookDetail) to refresh inventory
            window.dispatchEvent(new Event('inventory-updated'));
            fetchData();
        } catch (err: any) {
            alert(err.response?.data?.message || 'Failed to return book');
        }
    };

    const handleCancelReservation = async (resId: number) => {
        if (!window.confirm('Cancel this reservation?')) return;
        try {
            await api.delete(`/reservations/${resId}`);
            alert('Reservation cancelled.');
            fetchData();
        } catch (err) {
            alert('Failed to cancel reservation');
        }
    };

    // Derived Lists
    const activeLoans = loans.filter(l => l.status === 'ACTIVE' || l.status === 'OVERDUE');
    const loanHistory = loans.filter(l => l.status === 'RETURNED');
    const finedLoans = loans.filter(l => (l.fineAmount || 0) > 0);

    const formatCurrency = (amount?: number) => {
        return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(amount || 0);
    };

    if (loading) return <Box p={4} display="flex" justifyContent="center"><CircularProgress data-testid="loading-spinner" /></Box>;

    return (
        <Box p={3} data-testid="my-library-page">
            <Typography variant="h4" gutterBottom>My Library</Typography>
            {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}

            <Box sx={{ borderBottom: 1, borderColor: 'divider', mb: 3 }}>
                <Tabs value={tab} onChange={(_, v) => setTab(v)}>
                    <Tab label="Active Loans" data-testid="tab-active-loans" />
                    <Tab label="History" data-testid="tab-history" />
                    <Tab label="Fines & Fees" data-testid="tab-fines" />
                    <Tab label="Reservations" data-testid="tab-reservations" />
                </Tabs>
            </Box>

            {/* TAB 0: ACTIVE LOANS */}
            {tab === 0 && (
                <TableContainer component={Paper} data-testid="active-loans-panel">
                    <Table>
                        <TableHead>
                            <TableRow>
                                <TableCell>Book</TableCell>
                                <TableCell>Loan Date</TableCell>
                                <TableCell>Due Date</TableCell>
                                <TableCell>Status</TableCell>
                                <TableCell>Action</TableCell>
                            </TableRow>
                        </TableHead>
                        <TableBody>
                            {activeLoans.length === 0 ? (
                                <TableRow><TableCell colSpan={5} align="center">No active loans.</TableCell></TableRow>
                            ) : (
                                activeLoans.map((loan) => (
                                    <TableRow key={loan.id} data-testid={`active-loan-${loan.id}`}>
                                        <TableCell>{loan.bookTitle || loan.bookId}</TableCell>
                                        <TableCell>{new Date(loan.loanDate).toLocaleDateString()}</TableCell>
                                        <TableCell>{loan.dueDate ? new Date(loan.dueDate).toLocaleDateString() : '-'}</TableCell>
                                        <TableCell>
                                            <Chip
                                                label={loan.status}
                                                color={loan.status === 'OVERDUE' ? 'error' : 'primary'}
                                                size="small"
                                            />
                                        </TableCell>
                                        <TableCell>
                                            <Button
                                                variant="contained"
                                                color="secondary"
                                                size="small"
                                                onClick={() => handleReturnBook(loan.id)}
                                                data-testid={`return-btn-${loan.id}`}
                                            >
                                                Return Book
                                            </Button>
                                        </TableCell>
                                    </TableRow>
                                ))
                            )}
                        </TableBody>
                    </Table>
                </TableContainer>
            )}

            {/* TAB 1: HISTORY */}
            {tab === 1 && (
                <TableContainer component={Paper} data-testid="history-panel">
                    <Table>
                        <TableHead>
                            <TableRow>
                                <TableCell>Book</TableCell>
                                <TableCell>Loan Date</TableCell>
                                <TableCell>Return Date</TableCell>
                                <TableCell>Status</TableCell>
                            </TableRow>
                        </TableHead>
                        <TableBody>
                            {loanHistory.length === 0 ? (
                                <TableRow><TableCell colSpan={4} align="center">No loan history.</TableCell></TableRow>
                            ) : (
                                loanHistory.map((loan) => (
                                    <TableRow key={loan.id} data-testid={`history-loan-${loan.id}`}>
                                        <TableCell>{loan.bookTitle || loan.bookId}</TableCell>
                                        <TableCell>{new Date(loan.loanDate).toLocaleDateString()}</TableCell>
                                        <TableCell>{new Date(loan.returnDate!).toLocaleDateString()}</TableCell>
                                        <TableCell>
                                            <Chip label="RETURNED" color="default" size="small" />
                                        </TableCell>
                                    </TableRow>
                                ))
                            )}
                        </TableBody>
                    </Table>
                </TableContainer>
            )}

            {/* TAB 2: FINES */}
            {tab === 2 && (
                <Box data-testid="fines-panel">
                    {finedLoans.length === 0 ? (
                        <Alert severity="success">You have no fines!</Alert>
                    ) : (
                        <TableContainer component={Paper}>
                            <Table>
                                <TableHead>
                                    <TableRow>
                                        <TableCell>Loan ID</TableCell>
                                        <TableCell>Book ID</TableCell>
                                        <TableCell>Returned On</TableCell>
                                        <TableCell>Fine Amount</TableCell>
                                        <TableCell>Status</TableCell>
                                    </TableRow>
                                </TableHead>
                                <TableBody>
                                    {finedLoans.map((loan) => (
                                        <TableRow key={loan.id} data-testid={`fine-row-${loan.id}`}>
                                            <TableCell>{loan.id}</TableCell>
                                            <TableCell>{loan.bookId}</TableCell>
                                            <TableCell>
                                                {loan.returnDate ? new Date(loan.returnDate).toLocaleDateString() : 'Not Returned'}
                                            </TableCell>
                                            <TableCell sx={{ color: 'error.main', fontWeight: 'bold' }}>
                                                {formatCurrency(loan.fineAmount)}
                                            </TableCell>
                                            <TableCell>
                                                <Chip label="UNPAID" color="error" size="small" variant="outlined" />
                                            </TableCell>
                                        </TableRow>
                                    ))}
                                </TableBody>
                            </Table>
                        </TableContainer>
                    )}
                </Box>
            )}

            {/* TAB 3: RESERVATIONS */}
            {tab === 3 && (
                <TableContainer component={Paper} data-testid="reservations-panel">
                    <Table>
                        <TableHead>
                            <TableRow>
                                <TableCell>Book ID</TableCell>
                                <TableCell>Request Date</TableCell>
                                <TableCell>Status</TableCell>
                                <TableCell>Action</TableCell>
                            </TableRow>
                        </TableHead>
                        <TableBody>
                            {reservations.length === 0 ? (
                                <TableRow><TableCell colSpan={4} align="center">No reservations.</TableCell></TableRow>
                            ) : (
                                reservations.map((res) => (
                                    <TableRow key={res.id} data-testid={`reservation-${res.id}`}>
                                        <TableCell>{res.bookId}</TableCell>
                                        <TableCell>{new Date(res.requestDate).toLocaleDateString()}</TableCell>
                                        <TableCell>
                                            <Chip
                                                label={res.status}
                                                color={res.status === 'PENDING' ? 'warning' : 'default'}
                                                size="small"
                                            />
                                        </TableCell>
                                        <TableCell>
                                            {res.status === 'PENDING' && (
                                                <Button
                                                    color="error"
                                                    size="small"
                                                    onClick={() => handleCancelReservation(res.id)}
                                                    data-testid={`cancel-res-${res.id}`}
                                                >
                                                    Cancel
                                                </Button>
                                            )}
                                        </TableCell>
                                    </TableRow>
                                ))
                            )}
                        </TableBody>
                    </Table>
                </TableContainer>
            )}
        </Box>
    );
};

export default MyLibrary;
