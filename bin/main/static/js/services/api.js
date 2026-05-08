/**
 * API сервис для работы с чатами и профилем
 */
const API = {
    // Авторизация
    login: async (username, password) => {
        try {
            const response = await fetch(getApiPath('/auth/login'), {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ username, password })
            });
            if (!response.ok) {
                const errorText = await response.text();
                throw new Error(`HTTP ${response.status}: ${errorText}`);
            }
            const result = await response.json();
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
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ username, name, email, password })
            });
            if (!response.ok) {
                const errorText = await response.text();
                throw new Error(`HTTP ${response.status}: ${errorText}`);
            }
            const result = await response.json();
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
                headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                body: `email=${encodeURIComponent(email)}`
            });
            if (!response.ok) {
                const errorText = await response.text();
                throw new Error(`HTTP ${response.status}: ${errorText}`);
            }
            const result = await response.json();
            return result;
        } catch (error) {
            console.error('[API] Forgot password error:', error);
            throw error;
        }
    },

    // Профиль
    getMyProfile: async () => {
        const token = localStorage.getItem('authToken');
        const response = await fetch(getApiPath('/profiles'), {
            headers: { 'Authorization': `Bearer ${token}` }
        });
        if (!response.ok) throw new Error(`HTTP ${response.status}`);
        return response.json();
    },

    getChatsMeta: async () => {
        const token = localStorage.getItem('authToken');
        const response = await fetch(getApiPath('/chats/meta'), {
            headers: { 'Authorization': `Bearer ${token}` }
        });
        if (!response.ok) throw new Error(`HTTP ${response.status}`);
        return response.json();
    },

    getChatsBatch: async (chatIds) => {
        const token = localStorage.getItem('authToken');
        const idsParam = chatIds.join(',');
        const response = await fetch(getApiPath(`/chats/batch?ids=${idsParam}`), {
            headers: { 'Authorization': `Bearer ${token}` }
        });
        if (!response.ok) throw new Error(`HTTP ${response.status}`);
        return response.json();
    },

    getChatById: async (chatId) => {
        const token = localStorage.getItem('authToken');
        const response = await fetch(getApiPath(`/chats/${chatId}`), {
            headers: { 'Authorization': `Bearer ${token}` }
        });
        if (!response.ok) throw new Error(`HTTP ${response.status}`);
        return response.json();
    },

    updateSelfChatSettings: async (chatId, isPinned) => {
        const token = localStorage.getItem('authToken');
        const response = await fetch(getApiPath(`/chats/${chatId}/members/self`), {
            method: 'PUT',
            headers: {
                'Authorization': `Bearer ${token}`,
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ isPinned })
        });
        if (!response.ok) throw new Error(`HTTP ${response.status}`);
        return response.json();
    },

    createPersonalChat: async (tempId, otherUserId) => {
        const token = localStorage.getItem('authToken');
        const response = await fetch(getApiPath('/chats/create-personal'), {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${token}`,
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ tempId, otherUserId })
        });
        if (!response.ok) throw new Error(`HTTP ${response.status}`);
        return response.json();
    },

    // Синхронизация событий чатов
    syncChats: async (cursors) => {
        const token = localStorage.getItem('authToken');
        const response = await fetch(getApiPath('/chats/sync'), {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${token}`,
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ cursors })
        });
        if (!response.ok) throw new Error(`HTTP ${response.status}`);
        return response.json();
    },

    // Синхронизация пользовательских событий
    syncUserEvents: async (cursor) => {
        const token = localStorage.getItem('authToken');
        const response = await fetch(getApiPath(`/users/sync?cursor=${cursor}`), {
            method: 'POST',
            headers: { 'Authorization': `Bearer ${token}` }
        });
        if (!response.ok) throw new Error(`HTTP ${response.status}`);
        return response.json();
    },

        // Добавить в объект API после остальных методов
    getMessages: async (chatId, cursor, limit, direction) => {
        const token = localStorage.getItem('authToken');
        let url = getApiPath(`/chats/${chatId}/messages?limit=${limit}&direction=${direction}`);
        if (cursor) url += `&cursor=${cursor}`;
        const response = await fetch(url, {
            headers: { 'Authorization': `Bearer ${token}` }
        });
        if (!response.ok) throw new Error(`HTTP ${response.status}`);
        return response.json();
    }
};