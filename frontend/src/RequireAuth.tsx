import React from 'react';
import { Navigate, Outlet } from 'react-router-dom';
import { AuthService } from './services/AuthService';

const RequireAuth: React.FC<{ role?: 'ADMIN' | 'USER' }> = ({ role }) => {
    if (!AuthService.isAuthenticated()) {
        return <Navigate to="/login" replace />;
    }

    if (role && role === 'ADMIN' && !AuthService.isAdmin()) {
        return <Navigate to="/dashboard" replace />;
    }

    return <Outlet />;
};

export default RequireAuth;
