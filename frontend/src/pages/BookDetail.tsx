import React, { useEffect, useState } from 'react';
import {
    Box, Paper, Typography, Button, Divider, Chip,
    CircularProgress, Alert, Breadcrumbs, Link as MuiLink
} from '@mui/material';
import { useParams, Link } from 'react-router-dom';
import api from '../services/api';
import { AuthService } from '../services/AuthService';

interface BookCopy {
    id: number;
    status: string;
    barcode?: string;
}

interface BookDetail {
    id: number;
    title: string;
    author: string;
    isbn: string;
    quantity?: number;
    availableQuantity?: number;
    copies?: BookCopy[];
    createdAt: string;
}

const BookDetail: React.FC = () => {
    const { id } = useParams<{ id: string }>();
    const [book, setBook] = useState<BookDetail | null>(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    const [actionLoading, setActionLoading] = useState(false);

    useEffect(() => {
        fetchBookDetails();
    }, [id]);

    const fetchBookDetails = async () => {
        try {
            const response = await api.get(`/books/${id}`);
            const b = response.data;
            const copies = b.copies || [];
            const quantity = copies.length;
            const availableQuantity = copies.filter((c: any) => c.status === 'AVAILABLE').length;
            setBook({ ...b, quantity, availableQuantity });
        } catch (err) {
            setError('Book not found');
        } finally {
            setLoading(false);
        }
    };

    // Refresh details when inventory updates elsewhere (e.g., returns)
    useEffect(() => {
        const onInventoryUpdated = () => {
            fetchBookDetails();
        };
        window.addEventListener('inventory-updated', onInventoryUpdated);
        return () => window.removeEventListener('inventory-updated', onInventoryUpdated);
    }, [id]);

    const handleBorrow = async () => {
        if (!book) return;
        
        // Check if copies are present
        if (!book.copies || book.copies.length === 0) {
            alert('No copies available for this book.');
            return;
        }
        
        // Prefer AVAILABLE copy; if none, try RESERVED (READY_FOR_PICKUP expected for member)
        const availableCopy = book.copies.find((c: BookCopy) => c.status === 'AVAILABLE');
        const reservedCopy = !availableCopy ? book.copies.find((c: BookCopy) => c.status === 'RESERVED') : undefined;
        
        const user = AuthService.getCurrentUser();
        if (!user) {
            alert('Please login to borrow books.');
            return;
        }
        setActionLoading(true);
        try {
            await api.post('/loans/borrow', {
                member: { id: user.id },
                bookCopy: { id: (availableCopy || reservedCopy)!.id }
            });
            alert('Book borrowed successfully!');
            // Notify other pages to refresh inventory
            window.dispatchEvent(new Event('inventory-updated'));
            fetchBookDetails();
        } catch (err: any) {
            alert(err.response?.data?.message || 'Failed to borrow book.');
        } finally {
            setActionLoading(false);
        }
    };

    const handleReserve = async () => {
        if (!book) return;
        const user = AuthService.getCurrentUser();
        if (!user) {
            alert('Please login to reserve books.');
            return;
        }
        setActionLoading(true);
        try {
            await api.post('/reservations', {
                book: { id: book.id },
                member: { id: user.id },
                status: 'PENDING'
            });
            alert('Book reserved successfully!');
        } catch (err: any) {
            alert(err.response?.data?.message || 'Failed to reserve book.');
        } finally {
            setActionLoading(false);
        }
    };

    if (loading) return <Box p={4} display="flex" justifyContent="center"><CircularProgress /></Box>;
    if (error || !book) return <Box p={4}><Alert severity="error">{error}</Alert></Box>;

    return (
        <Box p={3} data-testid="book-detail-page">
            <Breadcrumbs sx={{ mb: 2 }}>
                <MuiLink component={Link} to="/dashboard/books" underline="hover" color="inherit">
                    Books
                </MuiLink>
                <Typography color="text.primary">{book.title}</Typography>
            </Breadcrumbs>

            <Paper elevation={2} sx={{ p: 4 }}>
                <Typography variant="h4" gutterBottom data-testid="book-title">
                    {book.title}
                </Typography>
                <Typography variant="h6" color="text.secondary" gutterBottom>
                    by {book.author}
                </Typography>

                <Box my={3}>
                    <Chip
                        label={`ISBN: ${book.isbn}`}
                        variant="outlined"
                        sx={{ mr: 1 }}
                    />
                    <Chip
                        label={(book.availableQuantity ?? 0) > 0 ? 'Available' : 'Out of Stock'}
                        color={(book.availableQuantity ?? 0) > 0 ? 'success' : 'error'}
                    />
                </Box>

                <Divider sx={{ my: 3 }} />

                <Box display="flex" justifyContent="space-between" maxWidth={400}>
                    <Typography><strong>Total Copies:</strong> {book.quantity ?? 0}</Typography>
                    <Typography data-testid="available-stock"><strong>Available:</strong> {book.availableQuantity ?? 0}</Typography>
                </Box>

                <Box mt={4} display="flex" gap={2}>
                    <Button
                        variant="contained"
                        size="large"
                        onClick={handleBorrow}
                        disabled={(
                            (book.availableQuantity ?? 0) === 0 &&
                            !(book.copies || []).some((c: BookCopy) => c.status === 'RESERVED')
                        ) || actionLoading}
                        data-testid="borrow-btn"
                    >
                        {(book.availableQuantity ?? 0) > 0
                            ? 'Borrow Book'
                            : (book.copies || []).some((c: BookCopy) => c.status === 'RESERVED')
                                ? 'Borrow Reserved Copy'
                                : 'Out of Stock'}
                    </Button>

                    {(book.availableQuantity ?? 0) === 0 && (
                        <Button
                            variant="outlined"
                            size="large"
                            onClick={handleReserve}
                            disabled={actionLoading}
                            data-testid="reserve-btn"
                        >
                            Reserve Book
                        </Button>
                    )}
                </Box>
            </Paper>
        </Box>
    );
};

export default BookDetail;
