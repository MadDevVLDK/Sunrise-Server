/**
 * API сервис для работы с чатами и профилем
 */
/**
 * API сервис для работы с чатами, профилем и авторизацией
 */
const API = {
    // Авторизация
    login: async (username, password) => {
        try {
            const response = await fetch(getApiPath('/auth/login'), {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({ username, password })
            });
            if (!response.ok) {
                // Получаем текст ошибки от сервера
                const errorText = await response.text();
                throw new Error(`HTTP ${response.status}: ${errorText}`);
            }
            const result = await response.json();
            console.log('[API] Login response:', result);
            return result;
        } catch (error) {
            console.error('[API] Login error:', error);
            throw error;
        }
    },

    register: async (username, name, email, password) => {
        try {
            const response = await fetch(getApiPath('/auth/register'), {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({ username, name, email, password })
            });
            if (!response.ok) {
                // Получаем текст ошибки от сервера
                const errorText = await response.text();
                throw new Error(`HTTP ${response.status}: ${errorText}`);
            }
            const result = await response.json();
            console.log('[API] Register response:', result);
            return result;
        } catch (error) {
            console.error('[API] Register error:', error);
            throw error;
        }
    },

    forgotPassword: async (email) => {
        try {
            const response = await fetch(getApiPath('/auth/change-password'), {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded'
                },
                body: `email=${encodeURIComponent(email)}`
            });
            if (!response.ok) {
                // Получаем текст ошибки от сервера
                const errorText = await response.text();
                throw new Error(`HTTP ${response.status}: ${errorText}`);
            }
            const result = await response.json();
            console.log('[API] Forgot password response:', result);
            return result;
        } catch (error) {
            console.error('[API] Forgot password error:', error);
            throw error;
        }
    },

    // Профиль
    getMyProfile: async () => {
        const token = localStorage.getItem('authToken');
        console.log('[API] Getting profile with token:', !!token);

        try {
            const response = await fetch(getApiPath('/profiles'), {
                headers: {
                    'Authorization': `Bearer ${token}`
                }
            });
            if (!response.ok) {
                // Получаем текст ошибки от сервера
                const errorText = await response.text();
                throw new Error(`HTTP ${response.status}: ${errorText}`);
            }
            const result = await response.json();
            console.log('[API] Profile response:', result);
            return result;
        } catch (error) {
            console.error('[API] Profile error:', error);
            throw error;
        }
    },

    // Чаты
    getUserChatIds: async () => {
        const token = localStorage.getItem('authToken');
        console.log('[API] Getting user chat IDs');

        try {
            const response = await fetch(`${getApiPath('/chats/ids')}`, {
                headers: { 'Authorization': `Bearer ${token}` }
            });
            if (!response.ok) {
                const errorText = await response.text();
                throw new Error(`HTTP ${response.status}: ${errorText}`);
            }
            const result = await response.json();
            console.log('[API] Chat IDs response:', result);
            return result;
        } catch (error) {
            console.error('[API] Chat IDs error:', error);
            throw error;
        }
    },

    getUserChats: async (isPinnedCursor = null, lastMsgIdCursor = null, chatIdCursor = null, limit = 20) => {
        const token = localStorage.getItem('authToken');
        let url = `${getApiPath('/chats')}?limit=${limit}`;
        if (isPinnedCursor !== null) url += `&isPinnedCursor=${isPinnedCursor}`;
        if (lastMsgIdCursor !== null) url += `&lastMsgIdCursor=${lastMsgIdCursor}`;
        if (chatIdCursor !== null) url += `&chatIdCursor=${chatIdCursor}`;

        console.log('[API] Getting chats from:', url);

        try {
            const response = await fetch(url, {
                headers: { 'Authorization': `Bearer ${token}` }
            });
            if (!response.ok) {
                // Получаем текст ошибки от сервера
                const errorText = await response.text();
                throw new Error(`HTTP ${response.status}: ${errorText}`);
            }
            const result = await response.json();
            console.log('[API] Chats response:', result);
            return result;
        } catch (error) {
            console.error('[API] Chats error:', error);
            throw error;
        }
    },

    getChatById: async (chatId) => {
        const token = localStorage.getItem('authToken');
        try {
            const response = await fetch(`${getApiPath('/chats')}/${chatId}`, {
                headers: { 'Authorization': `Bearer ${token}` }
            });
            if (!response.ok) {
                // Получаем текст ошибки от сервера
                const errorText = await response.text();
                throw new Error(`HTTP ${response.status}: ${errorText}`);
            }
            const result = await response.json();
            console.log('[API] Chat by id response:', result);
            return result;
        } catch (error) {
            console.error('[API] Get chat error:', error);
            throw error;
        }
    },

    // Пользователи профиля
    getProfilesByIds: async (userIds) => {
        const token = localStorage.getItem('authToken');
        try {
            const response = await fetch(getApiPath('/profiles/by-ids'), {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${token}`
                },
                body: JSON.stringify({ userIds })
            });
            if (!response.ok) {
                // Получаем текст ошибки от сервера
                const errorText = await response.text();
                throw new Error(`HTTP ${response.status}: ${errorText}`);
            }
            const result = await response.json();
            console.log('[API] Get profiles by ids response:', result);
            return result;
        } catch (error) {
            console.error('[API] Get profiles error:', error);
            throw error;
        }
    }
};