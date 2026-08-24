/**
 * API сервис для работы с сервером
 */
const API = {

    // ==================== ОБРАБОТКА ОТВЕТА ====================

    async handleResponse(response) {
        let result;
        try {
            result = await response.json();
        } catch (e) {
            throw new ApiError("INTERNAL_ERROR", "Не удалось разобрать ответ сервера", response.status);
        }

        if (!result.success) {
            const code = result.error?.code || "INTERNAL_ERROR";
            const message = result.error?.message || "";
            throw new ApiError(code, message, response.status);
        }

        return result.data;
    },

    async request(path, options = {}) {
        const token = localStorage.getItem('authToken');
        const headers = { ...options.headers };
        if (token) {
            headers['Authorization'] = `Bearer ${token}`;
        }
        if (options.body && !(options.body instanceof FormData)) {
            headers['Content-Type'] = headers['Content-Type'] || 'application/json';
        }

        const response = await fetch(getApiPath(path), { ...options, headers });

        if (response.status === 401) {
            // Токен истёк или недействителен
            AuthService.logout();
            throw new ApiError("UNAUTHORIZED", "Сессия истекла", 401);
        }

        if (response.status === 429) {
            throw new ApiError("RATE_LIMITED", "Слишком много запросов", 429);
        }

        return API.handleResponse(response);
    },

    // ==================== АВТОРИЗАЦИЯ ====================

    login: async (username, password) => {
        const response = await fetch(getApiPath('/auth/login'), {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username, password })
        });
        return API.handleResponse(response);
    },

    register: async (username, name, email, password) => {
        const response = await fetch(getApiPath('/auth/register'), {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username, name, email, password })
        });
        return API.handleResponse(response);
    },

    forgotPassword: async (email) => {
        const response = await fetch(getApiPath('/auth/change-password'), {
            method: 'PUT',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            body: `email=${encodeURIComponent(email)}`
        });
        return API.handleResponse(response);
    },

    // ==================== ПРОФИЛЬ ====================

    getMyProfile: async () => {
        return API.request('/profiles', { method: 'GET' });
    },

    updateProfile: async (username, name) => {
        return API.request('/profiles', {
            method: 'PUT',
            body: JSON.stringify({ username, name })
        });
    },

    deleteProfile: async () => {
        return API.request('/profiles', { method: 'DELETE' });
    },

    getOtherProfile: async (userId, light = null) => {
        let path = `/profiles/${userId}`;
        if (light !== null) path += `?light=${light}`;
        return API.request(path, { method: 'GET' });
    },

    getProfilesBatch: async (ids) => {
        const idsParam = ids.join(',');
        return API.request(`/profiles/batch?ids=${idsParam}`, { method: 'GET' });
    },

    // ==================== ЧАТЫ ====================

    createPersonalChat: async (tempId, otherUserId) => {
        return API.request('/chats/create-personal', {
            method: 'POST',
            body: JSON.stringify({ tempId, otherUserId })
        });
    },

    createGroupChat: async (tempId, chatName, chatDescription, members) => {
        return API.request('/chats/create-group', {
            method: 'POST',
            body: JSON.stringify({ tempId, chatName, chatDescription, members })
        });
    },

    updateChatInfo: async (chatId, chatName, chatDescription) => {
        return API.request(`/chats/${chatId}/info`, {
            method: 'PUT',
            body: JSON.stringify({ chatName, chatDescription })
        });
    },

    deleteChat: async (chatId) => {
        return API.request(`/chats/${chatId}`, { method: 'DELETE' });
    },

    getChatById: async (chatId) => {
        return API.request(`/chats/${chatId}`, { method: 'GET' });
    },

    getChatsMeta: async () => {
        return API.request('/chats/meta', { method: 'GET' });
    },

    getChatsBatch: async (chatIds) => {
        const idsParam = chatIds.join(',');
        return API.request(`/chats/batch?ids=${idsParam}`, { method: 'GET' });
    },

    getChatStats: async (chatId) => {
        return API.request(`/chats/${chatId}/stats`, { method: 'GET' });
    },

    syncChats: async (cursors) => {
        return API.request('/chats/sync', {
            method: 'POST',
            body: JSON.stringify({ cursors })
        });
    },

    // ==================== УЧАСТНИКИ ЧАТА ====================

    addChatMember: async (chatId, newUserId) => {
        return API.request(`/chats/${chatId}/members/add`, {
            method: 'POST',
            body: JSON.stringify({ newUserId })
        });
    },

    addChatMembers: async (chatId, members) => {
        return API.request(`/chats/${chatId}/members/add-many`, {
            method: 'POST',
            body: JSON.stringify({ members })
        });
    },

    updateChatMemberInfo: async (chatId, otherUserId, tag) => {
        return API.request(`/chats/${chatId}/members/${otherUserId}/info`, {
            method: 'PUT',
            body: JSON.stringify({ tag })
        });
    },

    updateAdminRights: async (chatId, otherUserId, isAdmin) => {
        return API.request(`/chats/${chatId}/members/${otherUserId}/admin-rights`, {
            method: 'PUT',
            body: JSON.stringify({ isAdmin })
        });
    },

    updateSelfChatSettings: async (chatId, isPinned) => {
        return API.request(`/chats/${chatId}/members/self`, {
            method: 'PUT',
            body: JSON.stringify({ isPinned })
        });
    },

    kickChatMember: async (chatId, otherUserId) => {
        return API.request(`/chats/${chatId}/members/${otherUserId}/kick`, { method: 'DELETE' });
    },

    leaveChat: async (chatId) => {
        return API.request(`/chats/${chatId}/members/leave`, { method: 'DELETE' });
    },

    getChatMembersPage: async (chatId, cursor, limit) => {
        let path = `/chats/${chatId}/members?limit=${limit}`;
        if (cursor !== null && cursor !== undefined && cursor !== '0' && cursor !== 0) {
            path += `&cursor=${cursor}`;
        }
        return API.request(path, { method: 'GET' });
    },

    getChatMembersBatch: async (chatId, ids) => {
        const idsParam = ids.join(',');
        return API.request(`/chats/${chatId}/members/batch?ids=${idsParam}`, { method: 'GET' });
    },

    // ==================== СООБЩЕНИЯ ====================

    getMessages: async (chatId, cursor, limit, direction) => {
        let path = `/chats/${chatId}/messages?limit=${limit}&direction=${direction}`;
        if (cursor !== null && cursor !== undefined && cursor !== '0' && cursor !== 0) {
            path += `&cursor=${cursor}`;
        }
        return API.request(path, { method: 'GET' });
    },

    getMessage: async (chatId, messageId) => {
        return API.request(`/chats/${chatId}/messages/${messageId}`, { method: 'GET' });
    },

    getMessagesBatch: async (chatId, ids) => {
        const idsParam = ids.join(',');
        return API.request(`/chats/${chatId}/messages/batch?ids=${idsParam}`, { method: 'GET' });
    },

    getMessageReads: async (chatId, messageId) => {
        return API.request(`/chats/${chatId}/messages/${messageId}/reads`, { method: 'GET' });
    },

    // ==================== ПОЛЬЗОВАТЕЛИ ====================

    
    getActiveUsersPage: async (filter, cursor, limit) => {
        let path = `/users?limit=${limit}`;
        if (filter) path += `&filter=${encodeURIComponent(filter)}`;
        if (cursor !== null && cursor !== undefined && cursor !== '0' && cursor !== 0) {
            path += `&cursor=${cursor}`;
        }
        return API.request(path, { method: 'GET' });
    },

    syncUserEvents: async (cursor) => {
        return API.request(`/users/sync?cursor=${cursor}`, { method: 'POST' });
    },
};


class ApiError extends Error {
    constructor(code, serverMessage, httpStatus) {
        super(getErrorMessage(code, serverMessage));
        this.name = 'ApiError';
        this.code = code;
        this.serverMessage = serverMessage;
        this.httpStatus = httpStatus;
        this.displayMessage = getErrorMessage(code);
    }
}