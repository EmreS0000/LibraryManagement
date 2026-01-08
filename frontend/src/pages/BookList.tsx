import React, { useEffect, useState } from 'react';
import {
    Box, Grid, Card, CardContent, Typography, TextField,
    InputAdornment, Button, Chip, CircularProgress, Alert, Pagination, 
    FormControl, InputLabel, Select, MenuItem, CardActions, Fade, Paper
} from '@mui/material';
import SearchIcon from '@mui/icons-material/Search';
import { LibraryBooks, Star, TrendingUp } from '@mui/icons-material';
import { useNavigate } from 'react-router-dom';
import api from '../services/api';
import { CategoryService, Category } from '../services/CategoryService';

interface Book {
    id: number;
    title: string;
    author: string;
    isbn: string;
    availableQuantity?: number;
    quantity?: number;
    copies?: { status: string }[];
    categoryNames: string[];
    publishYear?: number;
    pageCount?: number;
}

const BookList: React.FC = () => {
    const navigate = useNavigate();
    const [books, setBooks] = useState<Book[]>([]);
    const [categories, setCategories] = useState<Category[]>([]);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');

    const [searchTerm, setSearchTerm] = useState('');
    const [selectedCategory, setSelectedCategory] = useState<number | ''>('');
    const [page, setPage] = useState(1);
    const [totalPages, setTotalPages] = useState(1);
    const pageSize = 9;

    useEffect(() => {
        fetchCategories();
    }, []);

    useEffect(() => {
        fetchBooks();
    }, [page, searchTerm, selectedCategory]);

    const fetchCategories = async () => {
        try {
            const data = await CategoryService.getAllCategories();
            setCategories(data.filter(c => c.status === 'ACTIVE'));
        } catch (err) {
            console.error('Failed to categories', err);
        }
    };

    const fetchBooks = async () => {
        setLoading(true);
        try {
            let queryParams = `page=${page - 1}&size=${pageSize}`;
            if (searchTerm) queryParams += `&search=${searchTerm}`;
            if (selectedCategory) queryParams += `&categoryId=${selectedCategory}`;

            const endpoint = `/books?${queryParams}`;
            const response = await api.get(endpoint);
            
            const normalize = (b: any): Book => {
                const copies = b.copies || [];
                const quantity = copies.length;
                const availableQuantity = copies.filter((c: any) => c.status === 'AVAILABLE').length;
                return { ...b, quantity, availableQuantity };
            };

            if (response.data.content) {
                setBooks(response.data.content.map(normalize));
                setTotalPages(response.data.totalPages || 1);
            } else if (Array.isArray(response.data)) {
                setBooks(response.data.map(normalize));
                setTotalPages(1);
            } else {
                setBooks([]);
                setTotalPages(1);
            }
        } catch (err) {
            setError('Failed to load books.');
        } finally {
            setLoading(false);
        }
    };

    // Listen for global inventory updates (borrow/return) and refresh list
    useEffect(() => {
        const onInventoryUpdated = () => {
            fetchBooks();
        };
        window.addEventListener('inventory-updated', onInventoryUpdated);
        return () => window.removeEventListener('inventory-updated', onInventoryUpdated);
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [page, searchTerm, selectedCategory]);

    const handleSearch = (e: React.ChangeEvent<HTMLInputElement>) => {
        setSearchTerm(e.target.value);
        setPage(1);
    };

    return (
        <Box sx={{ flexGrow: 1 }}>
            {/* Header Section */}
            <Paper
                elevation={0}
                sx={{
                    p: 4,
                    mb: 4,
                    borderRadius: 3,
                    background: 'linear-gradient(135deg, #667eea15 0%, #764ba230 100%)',
                    border: '1px solid #667eea30',
                }}
            >
                <Box sx={{ display: 'flex', alignItems: 'center', mb: 2 }}>
                    <LibraryBooks sx={{ fontSize: 40, color: '#667eea', mr: 2 }} />
                    <Typography variant="h4" component="h1" fontWeight="bold" data-testid="book-list-header">
                        Books Catalog
                    </Typography>
                </Box>
                <Typography variant="body1" color="text.secondary">
                    Explore our extensive collection of books
                </Typography>
            </Paper>

            {/* Filters Section */}
            <Box sx={{ mb: 4, display: 'flex', flexDirection: { xs: 'column', md: 'row' }, gap: 2 }}>
                <FormControl sx={{ minWidth: { xs: '100%', md: 250 } }}>
                    <InputLabel>Category</InputLabel>
                    <Select
                        value={selectedCategory}
                        label="Category"
                        onChange={(e) => {
                            setSelectedCategory(e.target.value as number | '');
                            setPage(1);
                        }}
                        sx={{
                            borderRadius: 2,
                            '& .MuiOutlinedInput-notchedOutline': {
                                borderColor: '#667eea50',
                            },
                            '&:hover .MuiOutlinedInput-notchedOutline': {
                                borderColor: '#667eea',
                            },
                        }}
                    >
                        <MenuItem value="">
                            <em>All Categories</em>
                        </MenuItem>
                        {categories.map((c) => (
                            <MenuItem key={c.id} value={c.id}>
                                {c.name}
                            </MenuItem>
                        ))}
                    </Select>
                </FormControl>

                <TextField
                    placeholder="Search by title, author..."
                    variant="outlined"
                    fullWidth
                    value={searchTerm}
                    onChange={handleSearch}
                    InputProps={{
                        startAdornment: (
                            <InputAdornment position="start">
                                <SearchIcon sx={{ color: '#667eea' }} />
                            </InputAdornment>
                        ),
                    }}
                    sx={{
                        '& .MuiOutlinedInput-root': {
                            borderRadius: 2,
                            '& fieldset': {
                                borderColor: '#667eea50',
                            },
                            '&:hover fieldset': {
                                borderColor: '#667eea',
                            },
                        },
                    }}
                />
            </Box>

            {error && (
                <Alert severity="error" sx={{ mb: 2, borderRadius: 2 }}>
                    {error}
                </Alert>
            )}

            {loading ? (
                <Box display="flex" justifyContent="center" p={8}>
                    <CircularProgress data-testid="loading-spinner" sx={{ color: '#667eea' }} />
                </Box>
            ) : (
                <>
                    <Grid container spacing={3} data-testid="book-grid">
                        {books.map((book, index) => (
                            <Grid item xs={12} sm={6} md={4} key={book.id}>
                                <Fade in={true} timeout={300 + index * 100}>
                                    <Card
                                        data-testid={`book-item-${book.isbn}`}
                                        sx={{
                                            height: '100%',
                                            display: 'flex',
                                            flexDirection: 'column',
                                            borderRadius: 3,
                                            transition: 'all 0.3s ease',
                                            border: '1px solid #e0e0e0',
                                            '&:hover': {
                                                transform: 'translateY(-8px)',
                                                boxShadow: '0 12px 24px rgba(102, 126, 234, 0.3)',
                                                borderColor: '#667eea',
                                            },
                                        }}
                                    >
                                        <Box
                                            sx={{
                                                height: 8,
                                                background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
                                                borderRadius: '12px 12px 0 0',
                                            }}
                                        />
                                        <CardContent sx={{ flexGrow: 1, p: 3 }}>
                                            <Typography
                                                variant="h6"
                                                component="div"
                                                gutterBottom
                                                fontWeight="bold"
                                                sx={{
                                                    display: '-webkit-box',
                                                    overflow: 'hidden',
                                                    WebkitBoxOrient: 'vertical',
                                                    WebkitLineClamp: 2,
                                                    minHeight: '3em',
                                                }}
                                            >
                                                {book.title}
                                            </Typography>
                                            <Typography
                                                color="text.secondary"
                                                gutterBottom
                                                sx={{ display: 'flex', alignItems: 'center', mb: 2 }}
                                            >
                                                by <strong style={{ marginLeft: '4px' }}>{book.author}</strong>
                                            </Typography>

                                            {book.categoryNames && book.categoryNames.length > 0 && (
                                                <Box sx={{ mb: 2 }}>
                                                    {book.categoryNames.slice(0, 2).map((name) => (
                                                        <Chip
                                                            key={name}
                                                            label={name}
                                                            size="small"
                                                            sx={{
                                                                mr: 0.5,
                                                                mb: 0.5,
                                                                bgcolor: '#667eea15',
                                                                color: '#667eea',
                                                                fontWeight: 500,
                                                            }}
                                                        />
                                                    ))}
                                                    {book.categoryNames.length > 2 && (
                                                        <Chip
                                                            label={`+${book.categoryNames.length - 2}`}
                                                            size="small"
                                                            sx={{
                                                                bgcolor: '#f5f5f5',
                                                                fontWeight: 500,
                                                            }}
                                                        />
                                                    )}
                                                </Box>
                                            )}

                                            <Box display="flex" gap={1} flexWrap="wrap">
                                                {book.publishYear && (
                                                    <Chip
                                                        label={`Year: ${book.publishYear}`}
                                                        size="small"
                                                        variant="outlined"
                                                        sx={{ fontSize: '0.75rem' }}
                                                    />
                                                )}
                                                {book.pageCount && (
                                                    <Chip
                                                        label={`${book.pageCount} pages`}
                                                        size="small"
                                                        variant="outlined"
                                                        sx={{ fontSize: '0.75rem' }}
                                                    />
                                                )}
                                            </Box>
                                        </CardContent>
                                        <CardActions
                                            sx={{
                                                p: 2,
                                                pt: 0,
                                                display: 'flex',
                                                justifyContent: 'space-between',
                                                alignItems: 'center',
                                            }}
                                        >
                                            <Chip
                                                label={
                                                    (book.availableQuantity ?? 0) > 0
                                                        ? `${book.availableQuantity} Available`
                                                        : 'Out of Stock'
                                                }
                                                color={(book.availableQuantity ?? 0) > 0 ? 'success' : 'error'}
                                                size="small"
                                                sx={{ fontWeight: 600 }}
                                            />
                                            <Button
                                                size="medium"
                                                variant="contained"
                                                onClick={() => navigate(`/dashboard/books/${book.id}`)}
                                                data-testid={`view-book-${book.id}`}
                                                sx={{
                                                    background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
                                                    textTransform: 'none',
                                                    fontWeight: 600,
                                                    '&:hover': {
                                                        background: 'linear-gradient(135deg, #764ba2 0%, #667eea 100%)',
                                                    },
                                                }}
                                            >
                                                View Details
                                            </Button>
                                        </CardActions>
                                    </Card>
                                </Fade>
                            </Grid>
                        ))}
                    </Grid>

                    {books.length === 0 && !loading && (
                        <Paper
                            sx={{
                                p: 8,
                                textAlign: 'center',
                                borderRadius: 3,
                                bgcolor: '#f5f5f5',
                            }}
                        >
                            <LibraryBooks sx={{ fontSize: 80, color: '#ccc', mb: 2 }} />
                            <Typography variant="h6" color="text.secondary">
                                No books found
                            </Typography>
                            <Typography variant="body2" color="text.secondary" mt={1}>
                                Try adjusting your search or filter criteria
                            </Typography>
                        </Paper>
                    )}

                    {totalPages > 1 && (
                        <Box mt={6} display="flex" justifyContent="center">
                            <Pagination
                                count={totalPages}
                                page={page}
                                onChange={(_, value) => setPage(value)}
                                color="primary"
                                size="large"
                                data-testid="pagination-controls"
                                sx={{
                                    '& .MuiPaginationItem-root': {
                                        fontWeight: 600,
                                        '&.Mui-selected': {
                                            background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
                                        },
                                    },
                                }}
                            />
                        </Box>
                    )}
                </>
            )}
        </Box>
    );
};

export default BookList;
