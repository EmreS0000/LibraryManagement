import React from 'react';
import {
    AppBar, Toolbar, Typography, Button, Container, Box, Tab, Tabs, Badge
} from '@mui/material';
import { useNavigate, Routes, Route, useLocation, Navigate } from 'react-router-dom';
import BookList from './BookList';
import BookDetail from './BookDetail';
import MyLibrary from './MyLibrary';
import { LibraryBooks, LocalLibrary, Favorite, Logout } from '@mui/icons-material';

const Dashboard: React.FC = () => {
    const navigate = useNavigate();
    const location = useLocation();

    const handleLogout = () => {
        localStorage.clear();
        navigate('/login');
    };

    const currentTab = location.pathname.includes('library') ? 2 : (location.pathname.includes('loans') ? 1 : 0);

    const handleTabChange = (_: any, newValue: number) => {
        if (newValue === 0) navigate('/dashboard/books');
        if (newValue === 1) navigate('/dashboard/loans');
        if (newValue === 2) navigate('/dashboard/library');
    };

    return (
        <Box sx={{ flexGrow: 1, minHeight: '100vh', bgcolor: '#f8f9fa' }}>
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
                        Library LMS
                    </Typography>
                    <Button 
                        color="inherit" 
                        onClick={handleLogout} 
                        data-testid="logout-btn"
                        startIcon={<Logout />}
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

            <Container maxWidth="xl" sx={{ mt: 4 }}>
                <Tabs 
                    value={currentTab} 
                    onChange={handleTabChange} 
                    sx={{ 
                        mb: 3,
                        '& .MuiTab-root': {
                            fontWeight: 600,
                            fontSize: '1rem',
                            textTransform: 'none',
                        },
                        '& .Mui-selected': {
                            color: '#667eea',
                        },
                        '& .MuiTabs-indicator': {
                            backgroundColor: '#667eea',
                            height: 3,
                        },
                    }}
                    centered
                >
                    <Tab label="Books Catalog" icon={<LibraryBooks />} iconPosition="start" />
                    <Tab label="My Loans" icon={<LocalLibrary />} iconPosition="start" />
                    <Tab label="My Library" icon={<Favorite />} iconPosition="start" />
                </Tabs>

                <Routes>
                    <Route path="/" element={<Navigate to="books" replace />} />
                    <Route path="books" element={<BookList />} />
                    <Route path="books/:id" element={<BookDetail />} />
                    <Route path="loans" element={<MyLibrary />} />
                    <Route path="library" element={<MyLibrary />} />
                </Routes>
            </Container>
        </Box>
    );
};

export default Dashboard;
