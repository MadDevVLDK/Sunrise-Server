/**
 * Сервис для управления авторизацией
 */
const AuthService = {
    getToken: () => localStorage.getItem('authToken'),

    setToken: (token) => localStorage.setItem('authToken', token),

    removeToken: () => localStorage.removeItem('authToken'),

    isAuthenticated: () => !!localStorage.getItem('authToken'),

    logout: () => {
        console.log('[AuthService] logout');
        localStorage.removeItem('authToken');
        window.location.href = getFormsPath('/');
    },

    checkAuth: async () => {
        if (!AuthService.isAuthenticated()) {
            console.log('[AuthService] logout');
            window.location.href = getFormsPath('/');
            return false;
        }
        return true;
    }
};