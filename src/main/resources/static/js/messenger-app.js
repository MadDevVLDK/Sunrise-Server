/**
 * Главный компонент приложения мессенджера
 */
const MessengerApp = () => {
    const toBigInt = (id) => {
        if (id === undefined || id === null) return 0n;
        if (typeof id === 'bigint') return id;
        if (typeof id === 'number' && Number.isSafeInteger(id)) return BigInt(id);
        try {
            return BigInt(id);
        } catch (e) {
            console.warn('Failed to convert to BigInt:', id);
            return 0n;
        }
    };

    const isTempId = (id) => typeof id === 'string' && id.startsWith('temp_');

    const compareBigIntDesc = (a, b) => {
        const idA = toBigInt(a);
        const idB = toBigInt(b);
        if (idA === idB) return 0;
        return idA > idB ? -1 : 1;
    };

    const compareBigIntAsc = (a, b) => {
        const idA = toBigInt(a);
        const idB = toBigInt(b);
        if (idA === idB) return 0;
        return idA < idB ? -1 : 1;
    };

    const [user, setUser] = React.useState(null);
    const [chats, setChats] = React.useState([]);
    const [messages, setMessages] = React.useState({});
    const [isLoadingMessages, setIsLoadingMessages] = React.useState(false);
    const [hasMoreMessages, setHasMoreMessages] = React.useState({});
    const pendingMessagesRef = React.useRef(new Map());
    const [selectedChatId, setSelectedChatId] = React.useState(null);
    const [isLoadingChats, setIsLoadingChats] = React.useState(true);
    const [isMenuOpen, setIsMenuOpen] = React.useState(false);
    const stompClientRef = React.useRef(null);
    const subscriptionsRef = React.useRef(new Map());
    const chatLastEventIdRef = React.useRef(new Map());
    const userLastEventIdRef = React.useRef(0n);
    const isInitialized = React.useRef(false);
    const selectedChatIdRef = React.useRef(null);
    const [nextCursor, setNextCursor] = React.useState({});
    const loadingMoreRef = React.useRef(false);
    const currentChat = selectedChatId ? chats.find(c => c.id === selectedChatId) : null;
    const [scrollPositions, setScrollPositions] = React.useState({});
    const [hasMoreMessagesForward, setHasMoreMessagesForward] = React.useState({});
    const [nextCursorForward, setNextCursorForward] = React.useState({});
    const [initialScroll, setInitialScroll] = React.useState(null);
    const [isCreateChatOpen, setIsCreateChatOpen] = React.useState(false);
    const [isChatInfoOpen, setIsChatInfoOpen] = React.useState(false);

    const saveScrollPosition = React.useCallback((chatId, scrollTop) => {
        setScrollPositions(prev => ({ ...prev, [chatId.toString()]: scrollTop }));
    }, []);

    // ✅ Refs для актуальных данных в замыканиях WS
    const chatsRef = React.useRef([]);
    const userRef = React.useRef(null);
    const handleChatGlobalEventRef = React.useRef(null);
    const handleUserGlobalEventRef = React.useRef(null);

    // Синхронизируем refs при каждом обновлении состояния
    React.useEffect(() => { chatsRef.current = chats; }, [chats]);
    React.useEffect(() => { userRef.current = user; }, [user]);

    const [userProfileModal, setUserProfileModal] = React.useState({ isOpen: false, userId: null });
    const [isMyProfileOpen, setIsMyProfileOpen] = React.useState(false);

    const handleOpenUserProfile = (userId) => {
        setUserProfileModal({ isOpen: true, userId: userId });
    };

    const handleCloseUserProfile = () => {
        setUserProfileModal({ isOpen: false, userId: null });
    };

    // ===== ИНИЦИАЛИЗАЦИЯ =====
    React.useEffect(() => {
        const init = async () => {
            if (!AuthService.isAuthenticated()) {
                window.location.href = getFormsPath('/');
                return;
            }
            try {
                const profile = await API.getMyProfile();
                setUser({
                    ...profile,
                    id: toBigInt(profile.id)
                });
            } catch (e) {
                if (e instanceof ApiError && e.code === 'UNAUTHORIZED') {
                    AuthService.logout();
                } else {
                    Toast.error('Ошибка загрузки профиля');
                }
            }
        };
        init();
    }, []);

    React.useEffect(() => {
        if (!user || isInitialized.current) return;
        isInitialized.current = true;
        loadChats();
    }, [user]);

    // ===== CLEANUP (только при размонтировании компонента) =====
    React.useEffect(() => {
        return () => {
            console.log('[MessengerApp] Cleanup: отключаем STOMP');
            if (stompClientRef.current?.connected) {
            subscriptionsRef.current.forEach(sub => {
                try { sub.unsubscribe(); } catch (e) { /* уже отписан */ }
            });
            subscriptionsRef.current.clear();
            try { stompClientRef.current.disconnect(); } catch (e) { /* уже отключён */ }
            }
        };
    }, []); // ← пустой массив = только unmount

    // ===== СОРТИРОВКА ЧАТОВ =====
    const sortChats = (chatsArray) => {
        return [...chatsArray].sort((a, b) => {
            if (a.isPinned !== b.isPinned) return a.isPinned ? -1 : 1;
            const hasLastA = !!a.lastMessage?.id;
            const hasLastB = !!b.lastMessage?.id;
            if (hasLastA !== hasLastB) return hasLastA ? -1 : 1;
            if (hasLastA && hasLastB) return compareBigIntDesc(a.lastMessage.id, b.lastMessage.id);
            return compareBigIntDesc(a.id, b.id);
        });
    };

    // ===== ВСПОМОГАТЕЛЬНЫЕ ФУНКЦИИ =====
    const updateChatInList = (chatId, updater) => {
        setChats(prev => {
            const idx = prev.findIndex(c => c.id === chatId);
            if (idx === -1) return prev;
            const updated = updater(prev[idx]);
            const newChats = [...prev];
            newChats[idx] = updated;
            return sortChats(newChats);
        });
    };

    const removeChatFromList = (chatId) => {
        const key = chatId.toString();
        setChats(prev => prev.filter(c => c.id !== chatId));
        if (selectedChatIdRef.current === chatId) {
            selectedChatIdRef.current = null;
            setSelectedChatId(null);
        }
        const sub = subscriptionsRef.current.get(key);
        if (sub) {
            sub.unsubscribe();
            subscriptionsRef.current.delete(key);
        }
        chatLastEventIdRef.current.delete(key);
    };

    const addChatToList = (newChat) => {
        setChats(prev => {
            if (prev.some(c => c.id === newChat.id)) return prev;
            return sortChats([...prev, newChat]);
        });
    };

    const subscribeToChat = (chatId) => {
        const key = chatId.toString();
        if (!stompClientRef.current?.connected) return;
        if (subscriptionsRef.current.has(key)) return;
        const sub = stompClientRef.current.subscribe(`/topic/chats/${key}`, (message) => {
            try {
                const globalEvent = JSON.parse(message.body);
                // ✅ Вызываем актуальную версию через ref
                handleChatGlobalEventRef.current(chatId, globalEvent);
            } catch (e) { console.error(e); }
        });
        subscriptionsRef.current.set(key, sub);
    };

    // ===== ОБРАБОТКА СОБЫТИЙ ЧАТА =====
    // =========================== ОБРАБОТКА СОБЫТИЙ ЧАТА ===========================
    const handleChatGlobalEvent = (chatId, globalEvent) => {
        const { eventId, type, event } = globalEvent;
        // ✅ Используем userRef вместо user
        const currentUser = userRef.current;

        switch (type) {
            case 'MESSAGE_CREATED_FULL':
                const isMyMessage = BigInt(event.senderId) === currentUser?.id;

                updateChatInList(chatId, (chat) => ({
                    ...chat,
                    lastMessage: {
                        id: toBigInt(event.messageId),
                        text: event.text,
                        senderId: toBigInt(event.senderId),
                        sentAt: event.createdAt
                    },
                    unreadCount: (Number(chat.unreadCount) || 0) + (isMyMessage ? 0 : 1)
                }));

                setMessages(prev => {
                    const key = chatId.toString();
                    const current = prev[key];
                    if (current === undefined) return prev;

                    const tempIndex = current.findIndex(m => m.id === event.tempId);
                    if (tempIndex !== -1) {
                        const updatedMessages = [...current];
                        updatedMessages[tempIndex] = {
                            ...updatedMessages[tempIndex],
                            id: toBigInt(event.messageId),
                            isPending: false,
                            isDelivered: true,
                            isReadByAnyone: event.isReadByAnyone === true,
                        };
                        return { ...prev, [key]: updatedMessages };
                    } else {
                        if (current.some(m => m.id === toBigInt(event.messageId))) return prev;
                        const newMessage = {
                            id: toBigInt(event.messageId),
                            chatId: chatId,
                            senderId: toBigInt(event.senderId),
                            senderName: currentUser?.name || 'Пользователь',
                            text: event.text,
                            sentAt: event.createdAt,
                            isPending: false,
                            isDelivered: true,
                            isReadByAnyone: event.isReadByAnyone === true,
                        };
                        return { ...prev, [key]: [...current, newMessage] };
                    }
                });
                break;

            case 'MESSAGES_READ_UP_TO':
                const upToId = toBigInt(event.upToMessageId);
                const readerUserId = toBigInt(event.userId);

                if (readerUserId === currentUser?.id) {
                    const readCount = event.messagesReadCount || 0;
                    updateChatInList(chatId, (chat) => ({
                        ...chat,
                        unreadCount: Math.max(0, (Number(chat.unreadCount) || 0) - readCount),
                        lastReadMessageIdByMe: upToId > (chat.lastReadMessageIdByMe || 0n) ? upToId : chat.lastReadMessageIdByMe
                    }));
                } else {
                    updateChatInList(chatId, (chat) => ({
                        ...chat,
                        lastReadMessageIdByAnyone: upToId > (chat.lastReadMessageIdByAnyone || 0n) ? upToId : chat.lastReadMessageIdByAnyone
                    }));
                }

                setMessages(prev => {
                    const key = chatId.toString();
                    const current = prev[key];
                    if (current === undefined) return prev;
                    const updated = current.map(msg => {
                        if (msg.senderId === currentUser?.id && typeof msg.id !== 'string' && msg.id <= upToId) {
                            return { ...msg, isReadByAnyone: true };
                        }
                        return msg;
                    });
                    return { ...prev, [key]: updated };
                });
                break;

            case 'CHAT_UPDATED':
                updateChatInList(chatId, (chat) => ({
                    ...chat, name: event.newName, description: event.newDescription
                }));
                break;

            case 'CHAT_MEMBER_REMOVED':
                if (BigInt(event.userId) === currentUser?.id) {
                    removeChatFromList(chatId);
                }
                break;

            default:
                break;
        }

        const key = chatId.toString();
        chatLastEventIdRef.current.set(key, toBigInt(eventId));
    };

    // ===== ОБРАБОТКА СОБЫТИЙ ПОЛЬЗОВАТЕЛЯ =====
    const handleUserGlobalEvent = (globalEvent) => {
        const { eventId, type, event } = globalEvent;
        console.log('[WS] Событие пользователя:', type, event);

        switch (type) {
            case 'USER_CHAT_CREATED':
            case 'USER_CHAT_ADDED': {
                const chatIdStr = String(event.chatId);
                API.getChatById(chatIdStr).then(res => {
                    const newChat = {
                        ...res,
                        id: toBigInt(res.id),
                        isPinned: false,
                        unreadCount: 0,
                        lastMessage: null,
                        lastReadMessageIdByMe: null,
                        lastReadMessageIdByAnyone: null
                    };
                    addChatToList(newChat);
                    subscribeToChat(newChat.id);
                }).catch(err => {
                    console.error('Ошибка загрузки нового чата', err);
                });
                break;
            }
            case 'USER_CHAT_DELETED': {
                removeChatFromList(toBigInt(event.chatId));
                break;
            }
            case 'USER_CHAT_SETTINGS_CHANGED': {
                const targetChatId = toBigInt(event.chatId);
                setChats(prevChats => {
                    const existingIndex = prevChats.findIndex(c => c.id === targetChatId);
                    if (existingIndex !== -1) {
                        const updatedChat = { ...prevChats[existingIndex], isPinned: Boolean(event.isPinned) };
                        const newChats = [...prevChats];
                        newChats[existingIndex] = updatedChat;
                        return sortChats(newChats);
                    } else {
                        API.getChatById(targetChatId.toString()).then(res => {
                            const newChat = {
                                ...res,
                                id: toBigInt(res.id),
                                isPinned: Boolean(event.isPinned),
                                unreadCount: 0,
                                lastMessage: null,
                                lastReadMessageIdByMe: null,
                                lastReadMessageIdByAnyone: null
                            };
                            addChatToList(newChat);
                            subscribeToChat(newChat.id);
                        });
                        return prevChats;
                    }
                });
                break;
            }
            default:
                break;
        }
        userLastEventIdRef.current = userLastEventIdRef.current > toBigInt(eventId) ? userLastEventIdRef.current : toBigInt(eventId);
    };

    // ✅ Синхронизируем ref при каждом рендере
    handleChatGlobalEventRef.current = handleChatGlobalEvent;
    handleUserGlobalEventRef.current = handleUserGlobalEvent;

    // ===== СИНХРОНИЗАЦИЯ =====
    const syncAllChats = async () => {
        const cursors = [];
        for (const chat of chats) {
            const lastEventId = chatLastEventIdRef.current.get(chat.id) || 0;
            if (lastEventId > 0) {
                cursors.push({ chatId: chat.id, lastEventId });
            }
        }
        if (cursors.length === 0) return;

        try {
            const syncMap = await API.syncChats(cursors);
            for (const [chatId, syncData] of Object.entries(syncMap)) {
                const chatIdStr = String(chatId);
                const { events, hasMore, resetRequired } = syncData;
                if (resetRequired) {
                    console.warn(`Reset required for chat ${chatIdStr}, full reload`);
                    const fullRes = await API.getChatById(chatIdStr);
                    updateChatInList(toBigInt(chatIdStr), () => ({
                        ...fullRes,
                        id: toBigInt(fullRes.id)
                    }));
                    chatLastEventIdRef.current.set(chatIdStr, 0n);
                    continue;
                }
                for (const ev of events) {
                    handleChatGlobalEvent(toBigInt(chatIdStr), ev);
                }
                if (events.length) {
                    chatLastEventIdRef.current.set(chatIdStr, toBigInt(events[events.length - 1].eventId));
                }
            }
        } catch (err) {
            console.error('Ошибка синхронизации чатов', err);
            if (err instanceof ApiError) {
                Toast.error(err.displayMessage);
            }
        }
    };

    const syncUserEvents = async (lastEventId) => {
        if (lastEventId <= 0) return;
        try {
            const res = await API.syncUserEvents(lastEventId);
            const { events, hasMore, resetRequired } = res;
            if (resetRequired) {
                console.warn('Требуется полная перезагрузка пользовательских данных');
                userLastEventIdRef.current = 0n;
                await loadChats();
                return;
            }
            for (const ev of events) {
                handleUserGlobalEvent(ev);
            }
            if (hasMore && events.length) {
                await syncUserEvents(toBigInt(events[events.length - 1].eventId));
            }
        } catch (err) {
            console.error('Ошибка синхронизации пользователя', err);
        }
    };

    // ===== WEBSOCKET =====
    const initWebSocket = (initialChats) => {
        const token = AuthService.getToken();
        if (!token) return;

        const socketUrl = `${window.location.protocol}//${window.location.host}${getBasePath()}/ws?token=${token}`;
        console.info('[WS] Подключение к', socketUrl);

        const socket = new SockJS(socketUrl);
        const stompClient = Stomp.over(socket);
        stompClient.debug = (str) => console.debug('[STOMP]', str);

        stompClient.connect({},
            (frame) => {
                console.log('[WS] Соединение установлено', frame);
                stompClientRef.current = stompClient;

                stompClient.subscribe('/user/queue/user-events', (msg) => {
                    try {
                        const globalEvent = JSON.parse(msg.body);
                        // ✅ Вызываем актуальную версию через ref
                        handleUserGlobalEventRef.current(globalEvent);
                    } catch (e) { console.error(e); }
                });

                stompClient.subscribe('/user/queue/errors', (msg) => {
                    try {
                        const err = JSON.parse(msg.body);
                        const displayMessage = getErrorMessage(err.code, err.message);
                        console.error('[WS] Ошибка от сервера:', err.code, err.message);
                        Toast.error(displayMessage);
                    } catch (e) {
                        console.error('[WS] Не удалось разобрать ошибку:', e);
                    }
                });

                if (initialChats && initialChats.length) {
                    for (const chat of initialChats) {
                        subscribeToChat(chat.id);
                    }
                    const hasStoredEvents = Array.from(chatLastEventIdRef.current.values()).some(id => id > 0);
                    if (hasStoredEvents) {
                        syncAllChats();
                    }
                }

                if (userLastEventIdRef.current > 0) {
                    syncUserEvents(userLastEventIdRef.current);
                }
            },
            (err) => {
                console.error('[WS] Ошибка подключения', err);
                Toast.error('Не удалось подключиться к серверу. Обновления могут приходить с задержкой.');
            }
        );
    };

    // ===== ЗАГРУЗКА ЧАТОВ =====
    const loadChats = async () => {
        setIsLoadingChats(true);
        try {
            const metaList = await API.getChatsMeta();
            if (metaList.length === 0) {
                setChats([]);
                return;
            }

            const chatIds = metaList.map(m => m.id);
            const fullChatsRaw = await API.getChatsBatch(chatIds);

            let fullChats = fullChatsRaw.map(chat => {
                const meta = metaList.find(m => String(m.id) === String(chat.id));
                return {
                    ...chat,
                    id: toBigInt(chat.id),
                    isPinned: Boolean(meta?.isPinned),
                    unreadCount: Number(meta?.unreadCount) || 0,
                    lastReadMessageIdByMe: chat.lastReadMessageIdByMe ? toBigInt(chat.lastReadMessageIdByMe) : null,
                    lastReadMessageIdByAnyone: chat.lastReadMessageIdByAnyone ? toBigInt(chat.lastReadMessageIdByAnyone) : null,
                    lastMessage: chat.lastMessage ? {
                        id: toBigInt(chat.lastMessage.id),
                        text: chat.lastMessage.text,
                        senderId: toBigInt(chat.lastMessage.senderId),
                        sentAt: chat.lastMessage.sentAt
                    } : null
                };
            });

            setChats(sortChats(fullChats));

            for (const chat of fullChats) {
                const key = chat.id.toString();
                if (!chatLastEventIdRef.current.has(key)) {
                    chatLastEventIdRef.current.set(key, 0n);
                }
            }

            initWebSocket(fullChats);
        } catch (err) {
            if (err instanceof ApiError) {
                Toast.error(err.displayMessage);
            } else {
                Toast.error('Не удалось загрузить список чатов');
            }
        } finally {
            setIsLoadingChats(false);
        }
    };

    // ===== ЗАГРУЗКА СООБЩЕНИЙ =====
    // ===== ЗАГРУЗКА СООБЩЕНИЙ =====
    const loadMessages = async (chatId, cursor, direction = 'BACKWARD') => {
        if (chatId === undefined || isLoadingMessages) return;
        setIsLoadingMessages(true);
        const key = chatId.toString();

        try {
            // Не передаём курсор если он 0 или null
            const cursorParam = (cursor !== undefined && cursor !== null && cursor !== 0n && cursor !== '0')
                ? cursor.toString()
                : null;

            const res = await API.getMessages(key, cursorParam, 20, direction);

            let fetchedMessages = (res.messages || []).map(msg => ({
                ...msg,
                id: toBigInt(msg.id),
                senderId: toBigInt(msg.senderId),
                chatId: chatId,
            }));

            fetchedMessages = fetchedMessages.map(msg => ({
                ...msg,
                isDelivered: msg.isDelivered !== undefined ? msg.isDelivered : (msg.senderId === user.id),
                isReadByAnyone: msg.isReadByAnyone ?? false
            }));

            // Сервер возвращает:
            // - BACKWARD / без курсора: в порядке убывания (новые первые) → разворачиваем
            // - FORWARD: в порядке возрастания (старые первые) → не разворачиваем
            const isFirstLoad = (cursorParam === null);
            if (direction === 'BACKWARD' || isFirstLoad) {
                fetchedMessages = fetchedMessages.reverse();
            }

            setMessages(prev => {
                const existing = prev[key] || [];

                if (isFirstLoad) {
                    return { ...prev, [key]: fetchedMessages };
                }

                const existingIds = new Set(existing.map(m => m.id.toString()));
                const uniqueNew = fetchedMessages.filter(m => !existingIds.has(m.id.toString()));
                if (uniqueNew.length === 0) return prev;

                if (direction === 'BACKWARD') {
                    return { ...prev, [key]: [...uniqueNew, ...existing] };
                } else {
                    return { ...prev, [key]: [...existing, ...uniqueNew] };
                }
            });

            const hasMore = res.nextCursor !== null && res.nextCursor !== undefined;
            if (direction === 'BACKWARD' || isFirstLoad) {
                setHasMoreMessages(prev => ({ ...prev, [key]: hasMore }));
                if (hasMore && res.nextCursor) {
                    setNextCursor(prev => ({ ...prev, [key]: toBigInt(res.nextCursor) }));
                }
            } else {
                setHasMoreMessagesForward(prev => ({ ...prev, [key]: hasMore }));
                if (hasMore && res.nextCursor) {
                    setNextCursorForward(prev => ({ ...prev, [key]: toBigInt(res.nextCursor) }));
                }
            }
        } catch (err) {
            console.error('Ошибка загрузки сообщений:', err);
            if (err instanceof ApiError) {
                Toast.error(err.displayMessage);
            }
        } finally {
            setIsLoadingMessages(false);
        }
    };

    const loadMoreMessages = React.useCallback(async (chatId) => {
        const key = chatId.toString();
        if (loadingMoreRef.current || isLoadingMessages || !hasMoreMessages[key]) return;

        // Курсор = ID первого (самого старого) сообщения в текущем списке
        let cursor = nextCursor[key];
        if (!cursor) {
            const chatMessages = messages[key];
            if (chatMessages && chatMessages.length > 0) {
                cursor = chatMessages[0].id;
            } else {
                return;
            }
        }

        loadingMoreRef.current = true;
        try {
            await loadMessages(chatId, cursor, 'BACKWARD');
        } finally {
            loadingMoreRef.current = false;
        }
    }, [isLoadingMessages, hasMoreMessages, nextCursor, messages]);

    const loadMoreMessagesForward = React.useCallback(async (chatId) => {
        const key = chatId.toString();
        if (loadingMoreRef.current || isLoadingMessages || !hasMoreMessagesForward[key]) return;

        // Курсор = ID последнего (самого нового) сообщения в текущем списке
        let cursor = nextCursorForward[key];
        if (!cursor) {
            const chatMessages = messages[key];
            if (chatMessages && chatMessages.length > 0) {
                cursor = chatMessages[chatMessages.length - 1].id;
            } else {
                return;
            }
        }

        loadingMoreRef.current = true;
        try {
            await loadMessages(chatId, cursor, 'FORWARD');
        } finally {
            loadingMoreRef.current = false;
        }
    }, [isLoadingMessages, hasMoreMessagesForward, nextCursorForward, messages]);


    // ===== ОТПРАВКА СООБЩЕНИЯ =====
    const sendMessage = async (chatId, text) => {
        const key = chatId.toString();
        const tempId = `temp_${Date.now()}_${Math.random()}`;
        const tempMessage = {
            id: tempId,
            chatId: chatId,
            senderId: user.id,
            text: text,
            sentAt: new Date().toISOString(),
            isReadByAnyone: false,
            isPending: true,
            isDelivered: false,
            senderName: user.name,
        };

        setMessages(prev => {
            const current = prev[key] || [];
            return { ...prev, [key]: [...current, tempMessage] };
        });

        pendingMessagesRef.current.set(tempId, { chatId, text });

        if (stompClientRef.current?.connected) {
            stompClientRef.current.send(`/app/chats/${key}/messages/send`, {}, JSON.stringify({
                tempId: tempId,
                text: text
            }));
        } else {
            Toast.error('Нет соединения с сервером. Сообщение не отправлено.');
            setMessages(prev => {
                const current = prev[key] || [];
                const updated = current.map(m => m.id === tempId ? { ...m, isPending: false, text: m.text + ' (не отправлено)' } : m);
                return { ...prev, [key]: updated };
            });
            pendingMessagesRef.current.delete(tempId);
        }
    };

    // ===== ОТМЕТКА ПРОЧИТАННЫХ =====
    const markMessagesRead = (chatId, upToMessageId) => {
        // ✅ Используем chatsRef вместо chats (актуальные данные)
        const chat = chatsRef.current.find(c => c.id === chatId);
        if (!chat || upToMessageId <= (chat.lastReadMessageIdByMe || 0n)) return;
        if (!stompClientRef.current?.connected) return;
        stompClientRef.current.send(
            `/app/chats/${chatId.toString()}/messages/${upToMessageId.toString()}/up-to-read`, {}, {}
        );
    };

    // ===== ВЫБОР ЧАТА =====
    const handleSelectChat = async (chatId) => {
        const id = toBigInt(chatId);
        selectedChatIdRef.current = id;
        setSelectedChatId(id);

        const chat = chats.find(c => c.id === id);
        if (!chat) return;

        if (messages[id.toString()] === undefined) {
            const lastReadId = (chat?.lastReadMessageIdByMe && chat.lastReadMessageIdByMe !== 0n)
                ? chat.lastReadMessageIdByMe
                : 0n;
            const lastMessageId = chat?.lastMessage?.id || 0n;
            const needForward = lastMessageId > 0n && lastMessageId > lastReadId;

            if (lastReadId > 0n) {
                // Есть точка чтения: грузим старые ДО неё
                await loadMessages(id, lastReadId, 'BACKWARD');

                if (needForward) {
                    // Есть непрочитанные: грузим новые ПОСЛЕ неё
                    await loadMessages(id, lastReadId, 'FORWARD');
                }
            } else if (lastMessageId > 0n) {
                // ✅ Чат не читался: грузим последние (самые новые) сообщения
                // Сервер при cursor=null + BACKWARD вернёт последние 20
                await loadMessages(id, null, 'BACKWARD');
            }

            setInitialScroll({ chatId: id });
        }
    };

    // ===== ОБРАБОТЧИКИ =====
    const handleToggleMenu = () => setIsMenuOpen(!isMenuOpen);
    const handleLogout = () => AuthService.logout();
    const handleProfile = () => setIsMyProfileOpen(true);

    const handleProfileUpdated = (newUsername, newName) => {
        setUser(function(prev) {
            if (!prev) return prev;
            return Object.assign({}, prev, { username: newUsername, name: newName });
        });
    };

    const handleTogglePin = async (chatId, currentPinned) => {
        const toastId = Toast.loading(currentPinned ? 'Открепление чата...' : 'Закрепление чата...');
        try {
            await API.updateSelfChatSettings(chatId.toString(), !currentPinned);
            Toast.dismiss(toastId);
            updateChatInList(chatId, (chat) => ({ ...chat, isPinned: !currentPinned }));
        } catch (err) {
            Toast.dismiss(toastId);
            if (err instanceof ApiError) {
                Toast.error(err.displayMessage);
            } else {
                Toast.error('Не удалось закрепить чат');
            }
        }
    };

    const handleChatCreated = async (chatId) => {
        // Перезагружаем список чатов чтобы новый чат появился
        await loadChats();
    };

    const handleUpdateChatInfo = async (chatId, name, description) => {
        await API.updateChatInfo(chatId.toString(), name, description);
        updateChatInList(chatId, (chat) => ({ ...chat, name, description }));
    };

    const handleLeaveChat = async (chatId) => {
        const toastId = Toast.loading('Выход из чата...');
        try {
            await API.leaveChat(chatId.toString());
            Toast.dismiss(toastId);
            Toast.success('Вы покинули чат');
            removeChatFromList(chatId);
        } catch (e) {
            Toast.dismiss(toastId);
            if (e instanceof ApiError) {
                Toast.error(e.displayMessage);
            } else {
                Toast.error('Ошибка при выходе из чата');
            }
        }
    };

    const handleDeleteChat = async (chatId) => {
        const toastId = Toast.loading('Удаление чата...');
        try {
            await API.deleteChat(chatId.toString());
            Toast.dismiss(toastId);
            Toast.success('Чат удалён');
            removeChatFromList(chatId);
        } catch (e) {
            Toast.dismiss(toastId);
            if (e instanceof ApiError) {
                Toast.error(e.displayMessage);
            } else {
                Toast.error('Ошибка удаления чата');
            }
        }
    };

    const handleSendMessageToUser = async (userId) => {
        const toastId = Toast.loading('Создание чата...');
        try {
            const tempId = 'temp_' + Date.now();
            const chatId = await API.createPersonalChat(tempId, userId);
            Toast.dismiss(toastId);
            setUserProfileModal({ isOpen: false, userId: null });
            setIsChatInfoOpen(false);
            await loadChats();
            await handleSelectChat(chatId);
        } catch (e) {
            Toast.dismiss(toastId);
            if (e instanceof ApiError) {
                Toast.error(e.displayMessage);
            } else {
                Toast.error('Ошибка создания чата');
            }
        }
    };

    const handleBackToList = () => {
        selectedChatIdRef.current = null;
        setSelectedChatId(null);
    };


    // ===== РЕНДЕР =====
    if (!user) {
        return <div className="main-area">Загрузка...</div>;
    }

    return (
        <div className={`messenger-container ${selectedChatId ? 'mobile-chat-active' : ''}`}>
            <Sidebar
                user={user}
                chats={chats}
                selectedChatId={selectedChatId?.toString()}
                onSelectChat={handleSelectChat}
                isLoadingChats={isLoadingChats}
                isMenuOpen={isMenuOpen}
                onToggleMenu={handleToggleMenu}
                onLogout={handleLogout}
                onProfile={handleProfile}
                onTogglePin={handleTogglePin}
                onCreateChat={() => setIsCreateChatOpen(true)}
            />
            <ChatWindow
                chat={currentChat}
                user={user}
                messages={selectedChatId ? messages[selectedChatId.toString()] || [] : []}
                isLoading={isLoadingMessages}
                hasMore={hasMoreMessages[selectedChatId?.toString()] || false}
                hasMoreForward={hasMoreMessagesForward[selectedChatId?.toString()] || false}
                onLoadMore={() => selectedChatId && loadMoreMessages(selectedChatId)}
                onLoadMoreForward={() => selectedChatId && loadMoreMessagesForward(selectedChatId)}
                onSendMessage={sendMessage}
                onMarkRead={markMessagesRead}
                savedScrollTop={scrollPositions[selectedChatId?.toString()]}
                onScrollSave={saveScrollPosition}
                initialScroll={initialScroll}
                onInitialScrollDone={() => setInitialScroll(null)}
                onOpenChatInfo={() => setIsChatInfoOpen(true)}
                onBack={handleBackToList}
            />
            <CreateChatModal
                isOpen={isCreateChatOpen}
                onClose={() => setIsCreateChatOpen(false)}
                onChatCreated={handleChatCreated}
            />
            <ChatInfoModal
                isOpen={isChatInfoOpen}
                onClose={() => setIsChatInfoOpen(false)}
                chat={currentChat}
                user={user}
                onTogglePin={handleTogglePin}
                onLeaveChat={handleLeaveChat}
                onDeleteChat={handleDeleteChat}
                onUpdateChatInfo={handleUpdateChatInfo}
                onOpenUserProfile={handleOpenUserProfile}
            />
            <UserProfileModal
                isOpen={userProfileModal.isOpen}
                userId={userProfileModal.userId}
                onClose={handleCloseUserProfile}
                onSendMessage={handleSendMessageToUser}
            />
            <MyProfileModal
                isOpen={isMyProfileOpen}
                onClose={function() { setIsMyProfileOpen(false); }}
                user={user}
                onProfileUpdated={handleProfileUpdated}
            />
        </div>
    );
};

ReactDOM.render(<MessengerApp />, document.getElementById('root'));