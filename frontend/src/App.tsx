import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { CssBaseline, ThemeProvider, createTheme } from '@mui/material';
import Login from './pages/Login';
import Register from './pages/Register';
import BookList from './pages/BookList';
import BookDetail from './pages/BookDetail';
import MyLibrary from './pages/MyLibrary';
import Dashboard from './pages/Dashboard';
import AdminDashboard from './pages/AdminDashboard';
import RequireAuth from './RequireAuth';

const theme = createTheme({
    palette: {
        primary: { main: '#1976d2' },
        secondary: { main: '#dc004e' },
    },
});

function App() {
    return (
        <ThemeProvider theme={theme}>
            <CssBaseline />
            <Router>
                <Routes>
                    <Route path="/login" element={<Login />} />
                    <Route path="/register" element={<Register />} />

                    {/* User Routes */}
                    <Route element={<RequireAuth />}>
                        <Route path="/dashboard" element={<Dashboard />}>
                            <Route index element={<Navigate to="books" />} />
                            <Route path="books" element={<BookList />} />
                            <Route path="books/:id" element={<BookDetail />} />
                            <Route path="loans" element={<MyLibrary />} />
                        </Route>
                    </Route>

                    {/* Admin Routes */}
                    <Route element={<RequireAuth role="ADMIN" />}>
                        <Route path="/admin" element={<AdminDashboard />} />
                    </Route>

                    <Route path="/" element={<Navigate to="/login" />} />
                </Routes>
            </Router>
        </ThemeProvider>
    );
}

export default App;
