import React, { useEffect, useState } from 'react';
import {
    Box, AppBar, Toolbar, Typography, Button, Container, Grid, Paper,
    Table, TableBody, TableCell, TableContainer, TableHead, TableRow,
    IconButton, Dialog, DialogTitle, DialogContent, DialogActions, TextField,
    Select, MenuItem, InputLabel, FormControl, Tabs, Tab, Alert, Chip, OutlinedInput, 
    Checkbox, ListItemText, Card, CardContent, Fade, LinearProgress
} from '@mui/material';
import {
    Delete, Edit, CheckCircle, LibraryBooks, People, Category as CategoryIcon, LocalLibrary,
    TrendingUp, Warning, Add, Logout as LogoutIcon
} from '@mui/icons-material';
import { useNavigate } from 'react-router-dom';
import api from '../services/api';
import { AuthService } from '../services/AuthService';
import { CategoryService, Category } from '../services/CategoryService';
import { PublisherService, Publisher } from '../services/PublisherService';

interface Book {
    id: number;
    title: string;
    author: string;
    isbn: string;
    quantity?: number;
    availableQuantity?: number;
    copies?: { status: string }[];
    categoryIds: number[];
    publisherId?: number;
    publishYear?: number;
    pageCount?: number;
}

interface Loan {
    id: number;
    bookId?: number;
    bookTitle?: string;
    memberId?: number;
    memberEmail?: string;
    loanDate: string;
    dueDate: string;
    returnDate: string;
    status: 'ACTIVE' | 'RETURNED';
}

interface Statistics {
    totalBooks: number;
    totalLoans: number;
    activeLoans: number;
    overdueLoans: number;
    totalCategories: number;
    totalPublishers: number;
}

const AdminDashboard: React.FC = () => {
    const navigate = useNavigate();
    const [tab, setTab] = useState(0);
    const [books, setBooks] = useState<Book[]>([]);
    const [loans, setLoans] = useState<Loan[]>([]);
    const [categories, setCategories] = useState<Category[]>([]);
    const [publishers, setPublishers] = useState<Publisher[]>([]);
    const [statistics, setStatistics] = useState<Statistics>({
        totalBooks: 0,
        totalLoans: 0,
        activeLoans: 0,
        overdueLoans: 0,
        totalCategories: 0,
        totalPublishers: 0,
    });

    const [openBookDialog, setOpenBookDialog] = useState(false);
    const [openCategoryDialog, setOpenCategoryDialog] = useState(false);
    const [openPublisherDialog, setOpenPublisherDialog] = useState(false);
    const [openCopiesDialog, setOpenCopiesDialog] = useState(false);

    const [currentBook, setCurrentBook] = useState<Partial<Book>>({ categoryIds: [], quantity: 1 });
    const [currentCategory, setCurrentCategory] = useState<Partial<Category>>({ status: 'ACTIVE' });
    const [currentPublisher, setCurrentPublisher] = useState<Partial<Publisher>>({});

    const [error, setError] = useState('');
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        if (!AuthService.isAdmin()) {
            navigate('/dashboard');
            return;
        }
        fetchData();
        fetchCategories();
        fetchPublishers();
    }, [tab, navigate]);

    const fetchData = async () => {
        setLoading(true);
        if (tab === 0) await fetchBooks();
        if (tab === 1) await fetchLoans();
        if (tab === 2) await fetchCategories();
        if (tab === 3) await fetchPublishers();
        setLoading(false);
    };

    const fetchCategories = async () => {
        try {
            const data = await CategoryService.getAllCategories();
            setCategories(data);
            updateStatistics({ totalCategories: data.length });
        } catch (err) { console.error(err); }
    };

    const fetchPublishers = async () => {
        try {
            const data = await PublisherService.getAllPublishers();
            setPublishers(data);
            updateStatistics({ totalPublishers: data.length });
        } catch (err) { console.error(err); }
    };

    const normalizeBook = (b: any): Book => {
        const copies = b.copies || [];
        const quantity = copies.length;
        const availableQuantity = copies.filter((c: any) => c.status === 'AVAILABLE').length;
        return { ...b, quantity, availableQuantity };
    };

    const addCopies = async (bookId: number, count: number) => {
        for (let i = 0; i < count; i += 1) {
            await api.post(`/books/${bookId}/copies`);
        }
    };

    const fetchBooks = async () => {
        try {
            const res = await api.get('/books');
            const bookData = res.data.content || res.data;
            const normalized = (bookData || []).map(normalizeBook);
            setBooks(normalized);
            updateStatistics({ totalBooks: normalized.length });
        } catch (err) { console.error(err); }
    };

    const fetchLoans = async () => {
        try {
            const res = await api.get('/loans/admin/all');
            const normalizeLoan = (l: any): Loan => ({
                ...l,
                memberEmail: l.member?.email || '',
                memberId: l.member?.id,
                bookTitle: l.bookCopy?.book?.title || '',
                bookId: l.bookCopy?.book?.id,
            });

            const loanData = (res.data || []).map(normalizeLoan);
            setLoans(loanData);
            const activeLoans = loanData.filter((l: Loan) => l.status === 'ACTIVE').length;
            const overdueLoans = loanData.filter((l: Loan) => 
                l.status === 'ACTIVE' && new Date(l.dueDate) < new Date()
            ).length;
            updateStatistics({ 
                totalLoans: loanData.length, 
                activeLoans, 
                overdueLoans 
            });
        } catch (err) { console.error(err); }
    };

    const updateStatistics = (newStats: Partial<Statistics>) => {
        setStatistics(prev => ({ ...prev, ...newStats }));
    };

    const handleSaveBook = async () => {
        try {
            const payload = { ...currentBook } as any;
            if (currentBook.categoryIds) {
                payload.categories = currentBook.categoryIds.map((id) => ({ id }));
            }
            const desiredQuantity = payload.quantity ?? 0;
            delete payload.quantity;
            delete payload.availableQuantity;
            delete payload.categoryIds;

            if (currentBook.id) {
                await api.put(`/books/${currentBook.id}`, payload);
                const existing = books.find(b => b.id === currentBook.id);
                const currentQty = existing?.quantity ?? 0;
                if (desiredQuantity > currentQty) {
                    await addCopies(currentBook.id, desiredQuantity - currentQty);
                }
            } else {
                const res = await api.post('/books', payload);
                const savedId = res.data?.id;
                if (savedId && desiredQuantity > 0) {
                    await addCopies(savedId, desiredQuantity);
                }
            }
            setOpenBookDialog(false);
            fetchBooks();
        } catch (err: any) {
            setError('Failed to save book');
        }
    };

    const handleSaveCategory = async () => {
        try {
            if (currentCategory.id) {
                await CategoryService.updateCategory(currentCategory.id, currentCategory as Category);
            } else {
                await CategoryService.createCategory(currentCategory as Category);
            }
            setOpenCategoryDialog(false);
            fetchCategories();
        } catch (err: any) {
            setError('Failed to save category');
        }
    };

    const handleSavePublisher = async () => {
        try {
            if (currentPublisher.id) {
                await PublisherService.updatePublisher(currentPublisher.id, currentPublisher as Publisher);
            } else {
                await PublisherService.createPublisher(currentPublisher as any);
            }
            setOpenPublisherDialog(false);
            fetchPublishers();
        } catch (err: any) {
            setError('Failed to save publisher');
        }
    };

    const handleDeleteBook = async (id: number) => {
        if (window.confirm('Delete this book?')) {
            try {
                await api.delete(`/books/${id}`);
                fetchBooks();
            } catch (err) { alert('Failed to delete'); }
        }
    };

    const handleDeleteCategory = async (id: number) => {
        if (window.confirm('Delete this category?')) {
            try {
                await CategoryService.deleteCategory(id);
                fetchCategories();
            } catch (err) { alert('Failed to delete'); }
        }
    };

    const handleDeletePublisher = async (id: number) => {
        if (window.confirm('Delete this publisher?')) {
            try {
                await PublisherService.deletePublisher(id);
                fetchPublishers();
            } catch (err) { alert('Failed to delete'); }
        }
    };

    const handleReturnLoan = async (id: number) => {
        try {
            await api.post(`/loans/${id}/return`);
            fetchLoans();
        } catch (err) { alert('Failed to return loan'); }
    };

    const handleLogout = () => {
        AuthService.logout();
    };

    const StatCard = ({ title, value, icon, color, trend }: any) => (
        <Fade in={true}>
            <Card
                sx={{
                    height: '100%',
                    background: `linear-gradient(135deg, ${color}15 0%, ${color}30 100%)`,
                    border: `2px solid ${color}40`,
                    borderRadius: 3,
                    transition: 'all 0.3s ease',
                    '&:hover': {
                        transform: 'translateY(-5px)',
                        boxShadow: `0 8px 24px ${color}40`,
                    },
                }}
            >
                <CardContent>
                    <Box display="flex" justifyContent="space-between" alignItems="center">
                        <Box>
                            <Typography color="text.secondary" variant="body2" fontWeight={500}>
                                {title}
                            </Typography>
                            <Typography variant="h3" fontWeight="bold" color={color} mt={1}>
                                {value}
                            </Typography>
                            {trend && (
                                <Chip
                                    label={trend}
                                    size="small"
                                    icon={<TrendingUp />}
                                    sx={{ mt: 1, bgcolor: `${color}20`, color }}
                                />
                            )}
                        </Box>
                        <Box
                            sx={{
                                width: 60,
                                height: 60,
                                borderRadius: '50%',
                                bgcolor: `${color}20`,
                                display: 'flex',
                                alignItems: 'center',
                                justifyContent: 'center',
                            }}
                        >
                            {icon}
                        </Box>
                    </Box>
                </CardContent>
            </Card>
        </Fade>
    );

    return (
        <Box sx={{ flexGrow: 1, minHeight: '100vh', bgcolor: '#f5f5f5' }}>
            <AppBar 
                position="static" 
                sx={{
                    background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
                    boxShadow: '0 4px 20px rgba(102, 126, 234, 0.4)',
                }}
            >
                <Toolbar>
                    <LibraryBooks sx={{ mr: 2, fontSize: 32 }} />
                    <Typography variant="h5" component="div" sx={{ flexGrow: 1, fontWeight: 'bold' }}>
                        Library Admin Dashboard
                    </Typography>
                    <Button 
                        color="inherit" 
                        onClick={handleLogout}
                        startIcon={<LogoutIcon />}
                        sx={{
                            '&:hover': {
                                bgcolor: 'rgba(255,255,255,0.1)',
                            }
                        }}
                    >
                        Logout
                    </Button>
                </Toolbar>
            </AppBar>

            <Container maxWidth="xl" sx={{ mt: 4, mb: 4 }}>
                {/* Statistics Cards */}
                <Grid container spacing={3} mb={4}>
                    <Grid item xs={12} sm={6} md={3}>
                        <StatCard
                            title="Total Books"
                            value={statistics.totalBooks}
                            icon={<LibraryBooks sx={{ fontSize: 32, color: '#667eea' }} />}
                            color="#667eea"
                        />
                    </Grid>
                    <Grid item xs={12} sm={6} md={3}>
                        <StatCard
                            title="Active Loans"
                            value={statistics.activeLoans}
                            icon={<LocalLibrary sx={{ fontSize: 32, color: '#4caf50' }} />}
                            color="#4caf50"
                        />
                    </Grid>
                    <Grid item xs={12} sm={6} md={3}>
                        <StatCard
                            title="Overdue Loans"
                            value={statistics.overdueLoans}
                            icon={<Warning sx={{ fontSize: 32, color: '#f44336' }} />}
                            color="#f44336"
                        />
                    </Grid>
                    <Grid item xs={12} sm={6} md={3}>
                        <StatCard
                            title="Categories"
                            value={statistics.totalCategories}
                            icon={<CategoryIcon sx={{ fontSize: 32, color: '#ff9800' }} />}
                            color="#ff9800"
                        />
                    </Grid>
                </Grid>

                {/* Tabs */}
                <Paper sx={{ mb: 3, borderRadius: 2 }}>
                    <Tabs 
                        value={tab} 
                        onChange={(_, v) => setTab(v)}
                        variant="fullWidth"
                        sx={{
                            '& .MuiTab-root': {
                                fontWeight: 600,
                                fontSize: '1rem',
                            },
                        }}
                    >
                        <Tab label="Books" icon={<LibraryBooks />} iconPosition="start" />
                        <Tab label="Loans" icon={<LocalLibrary />} iconPosition="start" />
                        <Tab label="Categories" icon={<CategoryIcon />} iconPosition="start" />
                        <Tab label="Publishers" icon={<People />} iconPosition="start" />
                    </Tabs>
                </Paper>

                {loading && <LinearProgress sx={{ mb: 2 }} />}

                {/* Books Tab */}
                {tab === 0 && (
                    <Fade in={true}>
                        <Box>
                            <Button
                                variant="contained"
                                startIcon={<Add />}
                                onClick={() => {
                                    setCurrentBook({ categoryIds: [], quantity: 1 });
                                    setOpenBookDialog(true);
                                }}
                                sx={{
                                    mb: 2,
                                    background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
                                    '&:hover': {
                                        background: 'linear-gradient(135deg, #764ba2 0%, #667eea 100%)',
                                    },
                                }}
                            >
                                Add Book
                            </Button>
                            <TableContainer component={Paper} sx={{ borderRadius: 2 }}>
                                <Table>
                                    <TableHead sx={{ bgcolor: '#f5f5f5' }}>
                                        <TableRow>
                                            <TableCell><strong>Title</strong></TableCell>
                                            <TableCell><strong>Author</strong></TableCell>
                                            <TableCell><strong>ISBN</strong></TableCell>
                                            <TableCell><strong>Quantity</strong></TableCell>
                                            <TableCell align="right"><strong>Actions</strong></TableCell>
                                        </TableRow>
                                    </TableHead>
                                    <TableBody>
                                        {books.map((b) => (
                                            <TableRow key={b.id} hover>
                                                <TableCell>{b.title}</TableCell>
                                                <TableCell>{b.author}</TableCell>
                                                <TableCell>{b.isbn}</TableCell>
                                                <TableCell>
                                                    <Chip
                                                        label={`${b.availableQuantity ?? 0}/${b.quantity ?? 0}`}
                                                        color={(b.availableQuantity ?? 0) > 0 ? 'success' : 'error'}
                                                        size="small"
                                                    />
                                                </TableCell>
                                                <TableCell align="right">
                                                    <IconButton
                                                        onClick={() => {
                                                            setCurrentBook({
                                                                ...b,
                                                                categoryIds: (b as any).categories?.map((c: any) => c.id) || [],
                                                            });
                                                            setOpenBookDialog(true);
                                                        }}
                                                        color="primary"
                                                    >
                                                        <Edit />
                                                    </IconButton>
                                                    <IconButton
                                                        onClick={() => {
                                                            setCurrentBook(b);
                                                            setOpenCopiesDialog(true);
                                                        }}
                                                        color="info"
                                                        title="Manage Copies"
                                                    >
                                                        <CheckCircle />
                                                    </IconButton>
                                                    <IconButton
                                                        onClick={() => handleDeleteBook(b.id)}
                                                        color="error"
                                                    >
                                                        <Delete />
                                                    </IconButton>
                                                </TableCell>
                                            </TableRow>
                                        ))}
                                        {books.length === 0 && (
                                            <TableRow>
                                                <TableCell colSpan={5} align="center">
                                                    No books found
                                                </TableCell>
                                            </TableRow>
                                        )}
                                    </TableBody>
                                </Table>
                            </TableContainer>
                        </Box>
                    </Fade>
                )}

                {/* Loans Tab */}
                {tab === 1 && (
                    <Fade in={true}>
                        <TableContainer component={Paper} sx={{ borderRadius: 2 }}>
                            <Table>
                                <TableHead sx={{ bgcolor: '#f5f5f5' }}>
                                    <TableRow>
                                        <TableCell><strong>User</strong></TableCell>
                                        <TableCell><strong>Book</strong></TableCell>
                                        <TableCell><strong>Loan Date</strong></TableCell>
                                        <TableCell><strong>Due Date</strong></TableCell>
                                        <TableCell><strong>Status</strong></TableCell>
                                        <TableCell align="right"><strong>Action</strong></TableCell>
                                    </TableRow>
                                </TableHead>
                                <TableBody>
                                    {loans.map((l) => {
                                        const isOverdue = l.status === 'ACTIVE' && new Date(l.dueDate) < new Date();
                                        return (
                                            <TableRow
                                                key={l.id}
                                                hover
                                                sx={{
                                                    backgroundColor: isOverdue ? '#ffebee' : 'inherit',
                                                }}
                                            >
                                                <TableCell>{l.memberEmail || '-'}</TableCell>
                                                <TableCell>{l.bookTitle || '-'}</TableCell>
                                                <TableCell>
                                                    {new Date(l.loanDate).toLocaleDateString()}
                                                </TableCell>
                                                <TableCell>
                                                    {l.dueDate ? new Date(l.dueDate).toLocaleDateString() : 'N/A'}
                                                </TableCell>
                                                <TableCell>
                                                    {isOverdue ? (
                                                        <Chip label="OVERDUE" color="error" size="small" />
                                                    ) : (
                                                        <Chip
                                                            label={l.status}
                                                            color={l.status === 'ACTIVE' ? 'primary' : 'default'}
                                                            size="small"
                                                        />
                                                    )}
                                                </TableCell>
                                                <TableCell align="right">
                                                    {l.status === 'ACTIVE' && (
                                                        <IconButton
                                                            onClick={() => handleReturnLoan(l.id)}
                                                            color="success"
                                                            title="Mark Returned"
                                                        >
                                                            <CheckCircle />
                                                        </IconButton>
                                                    )}
                                                </TableCell>
                                            </TableRow>
                                        );
                                    })}
                                    {loans.length === 0 && (
                                        <TableRow>
                                            <TableCell colSpan={6} align="center">
                                                No loans found
                                            </TableCell>
                                        </TableRow>
                                    )}
                                </TableBody>
                            </Table>
                        </TableContainer>
                    </Fade>
                )}

                {/* Categories Tab */}
                {tab === 2 && (
                    <Fade in={true}>
                        <Box>
                            <Button
                                variant="contained"
                                startIcon={<Add />}
                                onClick={() => {
                                    setCurrentCategory({ status: 'ACTIVE' });
                                    setOpenCategoryDialog(true);
                                }}
                                sx={{
                                    mb: 2,
                                    background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
                                    '&:hover': {
                                        background: 'linear-gradient(135deg, #764ba2 0%, #667eea 100%)',
                                    },
                                }}
                            >
                                Add Category
                            </Button>
                            <TableContainer component={Paper} sx={{ borderRadius: 2 }}>
                                <Table>
                                    <TableHead sx={{ bgcolor: '#f5f5f5' }}>
                                        <TableRow>
                                            <TableCell><strong>Name</strong></TableCell>
                                            <TableCell><strong>Description</strong></TableCell>
                                            <TableCell><strong>Status</strong></TableCell>
                                            <TableCell align="right"><strong>Actions</strong></TableCell>
                                        </TableRow>
                                    </TableHead>
                                    <TableBody>
                                        {categories.map((c) => (
                                            <TableRow key={c.id} hover>
                                                <TableCell>{c.name}</TableCell>
                                                <TableCell>{c.description}</TableCell>
                                                <TableCell>
                                                    <Chip
                                                        label={c.status}
                                                        color={c.status === 'ACTIVE' ? 'success' : 'default'}
                                                        size="small"
                                                    />
                                                </TableCell>
                                                <TableCell align="right">
                                                    <IconButton
                                                        onClick={() => {
                                                            setCurrentCategory(c);
                                                            setOpenCategoryDialog(true);
                                                        }}
                                                        color="primary"
                                                    >
                                                        <Edit />
                                                    </IconButton>
                                                    <IconButton
                                                        onClick={() => handleDeleteCategory(c.id)}
                                                        color="error"
                                                    >
                                                        <Delete />
                                                    </IconButton>
                                                </TableCell>
                                            </TableRow>
                                        ))}
                                        {categories.length === 0 && (
                                            <TableRow>
                                                <TableCell colSpan={4} align="center">
                                                    No categories found
                                                </TableCell>
                                            </TableRow>
                                        )}
                                    </TableBody>
                                </Table>
                            </TableContainer>
                        </Box>
                    </Fade>
                )}

                {/* Publishers Tab */}
                {tab === 3 && (
                    <Fade in={true}>
                        <Box>
                            <Button
                                variant="contained"
                                startIcon={<Add />}
                                onClick={() => {
                                    setCurrentPublisher({});
                                    setOpenPublisherDialog(true);
                                }}
                                sx={{
                                    mb: 2,
                                    background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
                                    '&:hover': {
                                        background: 'linear-gradient(135deg, #764ba2 0%, #667eea 100%)',
                                    },
                                }}
                            >
                                Add Publisher
                            </Button>
                            <TableContainer component={Paper} sx={{ borderRadius: 2 }}>
                                <Table>
                                    <TableHead sx={{ bgcolor: '#f5f5f5' }}>
                                        <TableRow>
                                            <TableCell><strong>Name</strong></TableCell>
                                            <TableCell><strong>Country</strong></TableCell>
                                            <TableCell><strong>Founded</strong></TableCell>
                                            <TableCell align="right"><strong>Actions</strong></TableCell>
                                        </TableRow>
                                    </TableHead>
                                    <TableBody>
                                        {publishers.map((p) => (
                                            <TableRow key={p.id} hover>
                                                <TableCell>{p.name}</TableCell>
                                                <TableCell>{p.country}</TableCell>
                                                <TableCell>{p.foundedYear}</TableCell>
                                                <TableCell align="right">
                                                    <IconButton
                                                        onClick={() => {
                                                            setCurrentPublisher(p);
                                                            setOpenPublisherDialog(true);
                                                        }}
                                                        color="primary"
                                                    >
                                                        <Edit />
                                                    </IconButton>
                                                    <IconButton
                                                        onClick={() => handleDeletePublisher(p.id)}
                                                        color="error"
                                                    >
                                                        <Delete />
                                                    </IconButton>
                                                </TableCell>
                                            </TableRow>
                                        ))}
                                        {publishers.length === 0 && (
                                            <TableRow>
                                                <TableCell colSpan={4} align="center">
                                                    No publishers found
                                                </TableCell>
                                            </TableRow>
                                        )}
                                    </TableBody>
                                </Table>
                            </TableContainer>
                        </Box>
                    </Fade>
                )}

                {/* Book Dialog */}
                <Dialog open={openBookDialog} onClose={() => setOpenBookDialog(false)} maxWidth="sm" fullWidth>
                    <DialogTitle sx={{ bgcolor: '#667eea', color: 'white', fontWeight: 'bold' }}>
                        {currentBook.id ? 'Edit Book' : 'Add Book'}
                    </DialogTitle>
                    <DialogContent sx={{ mt: 2 }}>
                        <TextField
                            margin="dense"
                            label="Title"
                            fullWidth
                            value={currentBook.title || ''}
                            onChange={(e) => setCurrentBook({ ...currentBook, title: e.target.value })}
                        />
                        <TextField
                            margin="dense"
                            label="Author"
                            fullWidth
                            value={currentBook.author || ''}
                            onChange={(e) => setCurrentBook({ ...currentBook, author: e.target.value })}
                        />
                        <TextField
                            margin="dense"
                            label="ISBN"
                            fullWidth
                            value={currentBook.isbn || ''}
                            onChange={(e) => setCurrentBook({ ...currentBook, isbn: e.target.value })}
                        />
                        <TextField
                            margin="dense"
                            label="Quantity"
                            type="number"
                            fullWidth
                            value={currentBook.quantity ?? ''}
                            onChange={(e) => setCurrentBook({ ...currentBook, quantity: Number(e.target.value) })}
                        />
                        <Box display="flex" gap={2} mt={1}>
                            <TextField
                                margin="dense"
                                label="Publish Year"
                                type="number"
                                fullWidth
                                value={currentBook.publishYear || ''}
                                onChange={(e) =>
                                    setCurrentBook({ ...currentBook, publishYear: Number(e.target.value) })
                                }
                            />
                            <TextField
                                margin="dense"
                                label="Page Count"
                                type="number"
                                fullWidth
                                value={currentBook.pageCount || ''}
                                onChange={(e) =>
                                    setCurrentBook({ ...currentBook, pageCount: Number(e.target.value) })
                                }
                            />
                        </Box>
                        <FormControl fullWidth margin="dense">
                            <InputLabel>Publisher</InputLabel>
                            <Select
                                value={currentBook.publisherId || ''}
                                label="Publisher"
                                onChange={(e) =>
                                    setCurrentBook({ ...currentBook, publisherId: e.target.value as number })
                                }
                            >
                                <MenuItem value="">
                                    <em>None</em>
                                </MenuItem>
                                {publishers.map((p) => (
                                    <MenuItem key={p.id} value={p.id}>
                                        {p.name}
                                    </MenuItem>
                                ))}
                            </Select>
                        </FormControl>
                        <FormControl fullWidth margin="dense">
                            <InputLabel>Categories</InputLabel>
                            <Select
                                multiple
                                value={currentBook.categoryIds || []}
                                onChange={(e) =>
                                    setCurrentBook({
                                        ...currentBook,
                                        categoryIds:
                                            typeof e.target.value === 'string' ? [] : (e.target.value as number[]),
                                    })
                                }
                                input={<OutlinedInput label="Categories" />}
                                renderValue={(selected) => (
                                    <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 0.5 }}>
                                        {selected.map((value) => {
                                            const cat = categories.find((c) => c.id === value);
                                            return <Chip key={value} label={cat ? cat.name : value} />;
                                        })}
                                    </Box>
                                )}
                            >
                                {categories.map((category) => (
                                    <MenuItem key={category.id} value={category.id}>
                                        <Checkbox checked={(currentBook.categoryIds || []).indexOf(category.id) > -1} />
                                        <ListItemText primary={category.name} />
                                    </MenuItem>
                                ))}
                            </Select>
                        </FormControl>
                    </DialogContent>
                    <DialogActions>
                        <Button onClick={() => setOpenBookDialog(false)}>Cancel</Button>
                        <Button onClick={handleSaveBook} variant="contained">
                            Save
                        </Button>
                    </DialogActions>
                </Dialog>

                {/* Category Dialog */}
                <Dialog open={openCategoryDialog} onClose={() => setOpenCategoryDialog(false)}>
                    <DialogTitle sx={{ bgcolor: '#667eea', color: 'white', fontWeight: 'bold' }}>
                        {currentCategory.id ? 'Edit Category' : 'Add Category'}
                    </DialogTitle>
                    <DialogContent sx={{ mt: 2 }}>
                        <TextField
                            margin="dense"
                            label="Name"
                            fullWidth
                            value={currentCategory.name || ''}
                            onChange={(e) => setCurrentCategory({ ...currentCategory, name: e.target.value })}
                        />
                        <TextField
                            margin="dense"
                            label="Description"
                            fullWidth
                            multiline
                            rows={3}
                            value={currentCategory.description || ''}
                            onChange={(e) => setCurrentCategory({ ...currentCategory, description: e.target.value })}
                        />
                        <FormControl fullWidth margin="dense">
                            <InputLabel>Status</InputLabel>
                            <Select
                                value={currentCategory.status || 'ACTIVE'}
                                label="Status"
                                onChange={(e) =>
                                    setCurrentCategory({
                                        ...currentCategory,
                                        status: e.target.value as 'ACTIVE' | 'INACTIVE',
                                    })
                                }
                            >
                                <MenuItem value="ACTIVE">Active</MenuItem>
                                <MenuItem value="INACTIVE">Inactive</MenuItem>
                            </Select>
                        </FormControl>
                    </DialogContent>
                    <DialogActions>
                        <Button onClick={() => setOpenCategoryDialog(false)}>Cancel</Button>
                        <Button onClick={handleSaveCategory} variant="contained">
                            Save
                        </Button>
                    </DialogActions>
                </Dialog>

                {/* Publisher Dialog */}
                <Dialog open={openPublisherDialog} onClose={() => setOpenPublisherDialog(false)}>
                    <DialogTitle sx={{ bgcolor: '#667eea', color: 'white', fontWeight: 'bold' }}>
                        {currentPublisher.id ? 'Edit Publisher' : 'Add Publisher'}
                    </DialogTitle>
                    <DialogContent sx={{ mt: 2 }}>
                        <TextField
                            margin="dense"
                            label="Name"
                            fullWidth
                            value={currentPublisher.name || ''}
                            onChange={(e) => setCurrentPublisher({ ...currentPublisher, name: e.target.value })}
                        />
                        <TextField
                            margin="dense"
                            label="Country"
                            fullWidth
                            value={currentPublisher.country || ''}
                            onChange={(e) => setCurrentPublisher({ ...currentPublisher, country: e.target.value })}
                        />
                        <TextField
                            margin="dense"
                            label="Founded Year"
                            type="number"
                            fullWidth
                            value={currentPublisher.foundedYear || ''}
                            onChange={(e) =>
                                setCurrentPublisher({ ...currentPublisher, foundedYear: Number(e.target.value) })
                            }
                        />
                    </DialogContent>
                    <DialogActions>
                        <Button onClick={() => setOpenPublisherDialog(false)}>Cancel</Button>
                        <Button onClick={handleSavePublisher} variant="contained">
                            Save
                        </Button>
                    </DialogActions>
                </Dialog>

                <BookCopiesDialog
                    open={openCopiesDialog}
                    bookId={currentBook.id}
                    bookTitle={currentBook.title}
                    onClose={() => setOpenCopiesDialog(false)}
                />
            </Container>
        </Box>
    );
};

interface BookCopy {
    id: number;
    bookId: number;
    barcode: string;
    status: 'AVAILABLE' | 'LOANED' | 'DAMAGED' | 'LOST';
}

const BookCopiesDialog: React.FC<{
    open: boolean;
    bookId?: number;
    bookTitle?: string;
    onClose: () => void;
}> = ({ open, bookId, bookTitle, onClose }) => {
    const [copies, setCopies] = useState<BookCopy[]>([]);
    const [newBarcode, setNewBarcode] = useState('');

    useEffect(() => {
        if (open && bookId) {
            fetchCopies();
        }
    }, [open, bookId]);

    const fetchCopies = async () => {
        if (!bookId) return;
        try {
            const res = await api.get(`/books/${bookId}/copies`);
            setCopies(res.data);
        } catch (err) {
            console.error('Failed to fetch copies');
        }
    };

    const handleAddCopy = async () => {
        if (!bookId) return;
        try {
            await api.post(`/books/${bookId}/copies${newBarcode ? `?barcode=${newBarcode}` : ''}`);
            setNewBarcode('');
            fetchCopies();
        } catch (err) {
            alert('Failed to add copy');
        }
    };

    const handleDeleteCopy = async (copyId: number) => {
        if (!window.confirm('Delete this copy?')) return;
        try {
            await api.delete(`/books/copies/${copyId}`);
            fetchCopies();
        } catch (err) {
            alert('Failed to delete copy');
        }
    };

    return (
        <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
            <DialogTitle sx={{ bgcolor: '#667eea', color: 'white', fontWeight: 'bold' }}>
                Manage Copies: {bookTitle}
            </DialogTitle>
            <DialogContent sx={{ mt: 2 }}>
                <Box display="flex" gap={2} mb={2}>
                    <TextField
                        label="New Barcode (Optional)"
                        size="small"
                        fullWidth
                        value={newBarcode}
                        onChange={(e) => setNewBarcode(e.target.value)}
                        placeholder="Leave empty to auto-generate"
                    />
                    <Button variant="contained" onClick={handleAddCopy}>
                        Add Copy
                    </Button>
                </Box>
                <TableContainer component={Paper} variant="outlined">
                    <Table size="small">
                        <TableHead sx={{ bgcolor: '#f5f5f5' }}>
                            <TableRow>
                                <TableCell><strong>Barcode</strong></TableCell>
                                <TableCell><strong>Status</strong></TableCell>
                                <TableCell align="right"><strong>Action</strong></TableCell>
                            </TableRow>
                        </TableHead>
                        <TableBody>
                            {copies.map((c) => (
                                <TableRow key={c.id}>
                                    <TableCell>{c.barcode}</TableCell>
                                    <TableCell>
                                        <Chip
                                            label={c.status}
                                            color={c.status === 'AVAILABLE' ? 'success' : 'default'}
                                            size="small"
                                        />
                                    </TableCell>
                                    <TableCell align="right">
                                        <IconButton
                                            size="small"
                                            onClick={() => handleDeleteCopy(c.id)}
                                            disabled={c.status === 'LOANED'}
                                            color="error"
                                        >
                                            <Delete fontSize="small" />
                                        </IconButton>
                                    </TableCell>
                                </TableRow>
                            ))}
                            {copies.length === 0 && (
                                <TableRow>
                                    <TableCell colSpan={3} align="center">
                                        No copies found
                                    </TableCell>
                                </TableRow>
                            )}
                        </TableBody>
                    </Table>
                </TableContainer>
            </DialogContent>
            <DialogActions>
                <Button onClick={onClose}>Close</Button>
            </DialogActions>
        </Dialog>
    );
};

export default AdminDashboard;
