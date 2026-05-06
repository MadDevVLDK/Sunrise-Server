/**
 * Главный компонент приложения мессенджера
 */
const MessengerApp = () => {
    const [user, setUser] = React.useState(null);
    const [chats, setChats] = React.useState([]);
    const [messages, setMessages] = React.useState({});       // chatId -> массив сообщений
    const [isLoadingMessages, setIsLoadingMessages] = React.useState(false);
    const [hasMoreMessages, setHasMoreMessages] = React.useState({}); // chatId -> boolean
    const pendingMessagesRef = React.useRef(new Map()); // tempId -> {chatId, text}
    const [selectedChatId, setSelectedChatId] = React.useState(null);
    const [isLoadingChats, setIsLoadingChats] = React.useState(true);
    const [isMenuOpen, setIsMenuOpen] = React.useState(false);
    const [error, setError] = React.useState(null);
    const stompClientRef = React.useRef(null);
    const subscriptionsRef = React.useRef(new Map());
    const chatLastEventIdRef = React.useRef(new Map());
    const userLastEventIdRef = React.useRef(0);
    const isInitialized = React.useRef(false);
    const selectedChatIdRef = React.useRef(null);

    // === Функция сортировки чатов (вынесена) ===
    const sortChats = (chatsArray) => {
        return [...chatsArray].sort((a, b) => {
            // Закреплённые сверху
            if (a.isPinned !== b.isPinned) return a.isPinned ? -1 : 1;

            // Приоритет чатам с последним сообщением
            const hasLastA = !!a.lastMessage?.id;
            const hasLastB = !!b.lastMessage?.id;
            if (hasLastA !== hasLastB) return hasLastA ? -1 : 1;

            // Если у обоих есть сообщения – сортируем по ID сообщения (убывание)
            if (hasLastA && hasLastB) {
                return String(b.lastMessage.id).localeCompare(String(a.lastMessage.id));
            }

            // Если у обоих нет сообщений – сортируем по updatedAt (новые сверху)
            const getValidTime = (date) => {
                const time = new Date(date).getTime();
                return isNaN(time) ? 0 : time;
            };
            const timeA = getValidTime(a.updatedAt || a.createdAt);
            const timeB = getValidTime(b.updatedAt || b.createdAt);
            if (timeA !== timeB) {
                return timeB - timeA;
            }

            // Если даты одинаковы – по ID чата (убывание)
            return String(b.id).localeCompare(String(a.id));
        });
    };

    // === Вспомогательные функции ===
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
        setChats(prev => prev.filter(c => c.id !== chatId));
        if (selectedChatIdRef.current === chatId) {
            selectedChatIdRef.current = null;
            setSelectedChatId(null);
        }
        const sub = subscriptionsRef.current.get(chatId);
        if (sub) {
            sub.unsubscribe();
            subscriptionsRef.current.delete(chatId);
        }
        chatLastEventIdRef.current.delete(chatId);
    };

    const addChatToList = (newChat) => {
        setChats(prev => {
            if (prev.some(c => c.id === newChat.id)) return prev;
            const newChats = [...prev, newChat];
            console.log('Adding new chat:', newChat);
            console.log('Sorted chats:', sortChats([...prev, newChat]));
            return sortChats(newChats);
        });
    };

    const subscribeToChat = (chatId) => {
        const chatIdStr = String(chatId);
        if (!stompClientRef.current?.connected) return;
        if (subscriptionsRef.current.has(chatIdStr)) return;
        const sub = stompClientRef.current.subscribe(`/topic/chats/${chatIdStr}`, (message) => {
            try {
                const globalEvent = JSON.parse(message.body);
                handleChatGlobalEvent(chatIdStr, globalEvent);
            } catch(e) { console.error(e); }
        });
        subscriptionsRef.current.set(chatIdStr, sub);
    };

    const handleChatGlobalEvent = (chatId, globalEvent) => {
        const chatIdStr = String(chatId);
        const { eventId, type, event } = globalEvent;
        console.log(`[WS] Событие чата ${chatIdStr}:`, type, event);

        switch (type) {
            case 'MESSAGE_CREATED_FULL':
                const isMyMessage = String(event.senderId) === String(user?.id);
                updateChatInList(chatIdStr, (chat) => ({
                    ...chat,
                    lastMessage: {
                        id: String(event.messageId),
                        text: event.text,
                        senderId: String(event.senderId),
                        sentAt: event.createdAt
                    },
                    unreadCount: (Number(chat.unreadCount) || 0) + (isMyMessage ? 0 : 1)
                }));

                if (selectedChatIdRef.current === chatIdStr) {
                    setMessages(prev => {
                        const current = prev[chatIdStr] || [];
                        const tempIndex = current.findIndex(m => m.id === event.tempId);
                        if (tempIndex !== -1) {
                            const updatedMessages = [...current];
                            updatedMessages[tempIndex] = {
                                ...updatedMessages[tempIndex],
                                id: String(event.messageId),
                                isPending: false,
                                isDelivered: true,
                                readCount: isMyMessage ? 0 : 0,
                                isReadByMe: false,   // для своих сообщений не прочитано
                            };
                            return { ...prev, [chatIdStr]: updatedMessages };
                        } else {
                            if (current.some(m => String(m.id) === String(event.messageId))) return prev;
                            const newMessage = {
                                id: String(event.messageId),
                                chatId: chatIdStr,
                                senderId: String(event.senderId),
                                senderName: user?.name || 'Пользователь',
                                text: event.text,
                                sentAt: event.createdAt,
                                isPending: false,
                                isDelivered: true,
                                readCount: isMyMessage ? 0 : 0,
                                isReadByMe: false,
                            };
                            return { ...prev, [chatIdStr]: [...current, newMessage] };
                        }
                    });

                    if (!isMyMessage) {
                        markMessagesRead(chatIdStr, event.messageId);
                    }
                }
                break;
            case 'MESSAGES_READ_UP_TO':
                // Событие о прочтении сообщений до указанного ID
                if (String(event.userId) === String(user?.id)) {
                    // Текущий пользователь прочитал – обнуляем unreadCount
                    updateChatInList(chatIdStr, (chat) => ({ ...chat, unreadCount: 0 }));
                    if (selectedChatIdRef.current === chatIdStr) {
                        setMessages(prev => {
                            const current = prev[chatIdStr] || [];
                            const updated = current.map(msg => {
                                // Для чужих сообщений: isReadByMe не используется (оставляем без изменений)
                                return msg;
                            });
                            return { ...prev, [chatIdStr]: updated };
                        });
                    }
                } else {
                    // Другой пользователь прочитал – помечаем наши сообщения как прочитанные
                    if (selectedChatIdRef.current === chatIdStr) {
                        setMessages(prev => {
                            const current = prev[chatIdStr] || [];
                            const updated = current.map(msg => {
                                if (msg.senderId === user?.id && msg.id <= String(event.upToMessageId)) {
                                    return { 
                                        ...msg, 
                                        isReadByMe: true,
                                        readCount: (Number(msg.readCount) || 0) + 1
                                    };
                                }
                                return msg;
                            });
                            return { ...prev, [chatIdStr]: updated };
                        });
                    }
                }
                break;
            case 'CHAT_UPDATED':
                updateChatInList(chatIdStr, (chat) => ({ ...chat, name: event.newName, description: event.newDescription }));
                break;
            case 'CHAT_MEMBER_REMOVED':
                if (String(event.userId) === String(user?.id)) {
                    removeChatFromList(chatIdStr);
                }
                break;
            default:
                break;
        }
        chatLastEventIdRef.current.set(chatIdStr, eventId);
    };

    const handleUserGlobalEvent = (globalEvent) => {
        const { eventId, type, event } = globalEvent;
        console.log('[WS] Событие пользователя:', type, event);

        switch (type) {
            case 'USER_CHAT_CREATED':
            case 'USER_CHAT_ADDED': {
                const chatIdStr = String(event.chatId);
                API.getChatById(chatIdStr).then(res => {
                    if (res.success) {
                        const newChat = { ...res.data, id: String(res.data.id) };
                        addChatToList(newChat);
                        subscribeToChat(newChat.id);
                    }
                }).catch(err => console.error('Ошибка загрузки нового чата', err));
                break;
            }
            case 'USER_CHAT_DELETED': {
                removeChatFromList(String(event.chatId));
                break;
            }
            case 'USER_CHAT_SETTINGS_CHANGED': {
                const targetChatId = String(event.chatId);
                setChats(prevChats => {
                    const existingIndex = prevChats.findIndex(c => c.id === targetChatId);
                    if (existingIndex !== -1) {
                        const updatedChat = { ...prevChats[existingIndex], isPinned: Boolean(event.isPinned) };
                        const newChats = [...prevChats];
                        newChats[existingIndex] = updatedChat;
                        return sortChats(newChats);
                    } else {
                        API.getChatById(Number(targetChatId)).then(res => {
                            if (res.success) {
                                const newChat = {
                                    ...res.data,
                                    id: String(res.data.id),
                                    isPinned: Boolean(event.isPinned),
                                    unreadCount: 0,
                                    lastMessage: null
                                };
                                setChats(prev => {
                                    if (prev.some(c => c.id === targetChatId)) return prev;
                                    const newChats = [...prev, newChat];
                                    return sortChats(newChats);
                                });
                            }
                        });
                        return prevChats;
                    }
                });
                break;
            }
            default:
                break;
        }
        userLastEventIdRef.current = Math.max(userLastEventIdRef.current, eventId);
    };

    // === Синхронизация (без изменений) ===
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
            const res = await API.syncChats(cursors);
            if (!res.success) throw new Error(res.error);
            const syncMap = res.data;
            for (const [chatId, syncData] of Object.entries(syncMap)) {
                const chatIdStr = String(chatId);
                const { events, hasMore, resetRequired } = syncData;
                if (resetRequired) {
                    console.warn(`Reset required for chat ${chatIdStr}, full reload`);
                    const fullRes = await API.getChatById(chatIdStr);
                    if (fullRes.success) {
                        updateChatInList(chatIdStr, () => fullRes.data);
                        chatLastEventIdRef.current.set(chatIdStr, 0);
                    }
                    continue;
                }
                for (const ev of events) {
                    handleChatGlobalEvent(chatIdStr, ev);
                }
                if (hasMore && events.length) {
                    const newLastId = events[events.length-1].eventId;
                    chatLastEventIdRef.current.set(chatIdStr, newLastId);
                } else if (events.length) {
                    chatLastEventIdRef.current.set(chatIdStr, events[events.length-1].eventId);
                }
            }
        } catch (err) {
            console.error('Ошибка синхронизации чатов', err);
        }
    };

    const syncUserEvents = async (lastEventId) => {
        if (lastEventId <= 0) return;
        try {
            const res = await API.syncUserEvents(lastEventId);
            if (!res.success) throw new Error(res.error);
            const { events, hasMore, resetRequired } = res.data;
            if (resetRequired) {
                console.warn('Требуется полная перезагрузка пользовательских данных');
                userLastEventIdRef.current = 0;
                await loadChats();
                return;
            }
            for (const ev of events) {
                handleUserGlobalEvent(ev);
            }
            if (hasMore && events.length) {
                const newLastId = events[events.length-1].eventId;
                await syncUserEvents(newLastId);
            } else if (events.length) {
                userLastEventIdRef.current = events[events.length-1].eventId;
            }
        } catch (err) {
            console.error('Ошибка синхронизации пользователя', err);
        }
    };

    const loadChats = async () => {
        setIsLoadingChats(true);
        try {
            const metaRes = await API.getChatsMeta();
            if (!metaRes.success) throw new Error(metaRes.error);
            const metaList = metaRes.data || [];
            if (metaList.length === 0) {
                setChats([]);
                return;
            }
            const chatIds = metaList.map(m => m.id);
            const batchRes = await API.getChatsBatch(chatIds);
            if (!batchRes.success) throw new Error(batchRes.error);
            let fullChats = batchRes.data || [];

            fullChats = fullChats.map(chat => {
                const meta = metaList.find(m => String(m.id) === String(chat.id));
                return {
                    ...chat,
                    id: String(chat.id),
                    isPinned: Boolean(meta?.isPinned),
                    unreadCount: Number(meta?.unreadCount) || 0,
                    lastMessage: chat.lastMessage ? {
                        id: String(chat.lastMessage.id),
                        text: chat.lastMessage.text,
                        senderId: String(chat.lastMessage.senderId),
                        sentAt: chat.lastMessage.sentAt
                    } : null
                };
            });

            fullChats = sortChats(fullChats);
            setChats(fullChats);

            for (const chat of fullChats) {
                if (!chatLastEventIdRef.current.has(chat.id)) {
                    chatLastEventIdRef.current.set(chat.id, 0);
                }
            }

            initWebSocket(fullChats);
        } catch (err) {
            setError(err.message);
        } finally {
            setIsLoadingChats(false);
        }
    };

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
                        handleUserGlobalEvent(globalEvent);
                    } catch(e) { console.error(e); }
                });
                stompClient.subscribe('/user/queue/errors', (msg) => {
                    try {
                        const err = JSON.parse(msg.body);
                        console.error('[WS] Ошибка от сервера:', err);
                        setError(err.message);
                    } catch(e) {}
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
                setError('Не удалось подключиться к WebSocket. Некоторые обновления могут не приходить.');
            }
        );
    };

    const loadMessages = async (chatId, cursor, direction = 'BACKWARD') => {
        const chatIdStr = String(chatId);
        if (!chatIdStr) return;
        setIsLoadingMessages(true);
        try {
            const res = await API.getMessages(chatId, cursor, 20, direction);
            if (!res.success) throw new Error(res.error);
            let fetchedMessages = (res.data.messages || []).map(msg => ({
                ...msg,
                id: String(msg.id),
                senderId: String(msg.senderId),
                isPending: false,
                isDelivered: true,
                isReadByMe: String(msg.senderId) === String(user?.id) ? (msg.readCount > 0) : false,
                readCount: msg.readCount || 0
            }));
            if (direction === 'BACKWARD') {
                fetchedMessages = fetchedMessages.reverse();
            }
            setMessages(prev => {
                const existing = prev[chatIdStr] || [];
                let newMessages;
                if (direction === 'BACKWARD') {
                    if (!cursor) {
                        newMessages = fetchedMessages;
                    } else {
                        newMessages = [...fetchedMessages, ...existing];
                    }
                } else {
                    newMessages = [...existing, ...fetchedMessages];
                }
                return { ...prev, [chatIdStr]: newMessages };
            });
            setHasMoreMessages(prev => ({ ...prev, [chatIdStr]: fetchedMessages.length === 20 }));
        } catch (err) {
            console.error('Ошибка загрузки сообщений:', err);
        } finally {
            setIsLoadingMessages(false);
        }
    };

    const loadMoreMessages = (chatId) => {
        const chatIdStr = String(chatId);
        if (!chatIdStr || isLoadingMessages || !hasMoreMessages[chatIdStr]) return;
        const firstMessage = messages[chatIdStr]?.[0];
        if (firstMessage) {
            loadMessages(chatId, firstMessage.id, 'BACKWARD');
        }
    };

    const sendMessage = async (chatId, text) => {
        const chatIdStr = String(chatId);
        console.log('sendMessage: chatIdStr =', chatIdStr);
        const tempId = `temp_${Date.now()}_${Math.random()}`;
        const tempMessage = {
            id: tempId,
            chatId: chatIdStr,
            senderId: user.id,
            text: text,
            sentAt: new Date().toISOString(),
            isPending: true,
            isDelivered: false,
            isReadByMe: false,
            senderName: user.name,
            readCount: 0,
        };
        setMessages(prev => {
            const current = prev[chatIdStr] || [];
            console.log('sendMessage: установка messages, текущий массив:', current);
            const newMap = { ...prev, [chatIdStr]: [...current, tempMessage] };
            console.log('sendMessage: новый state messages keys:', Object.keys(newMap));
            return newMap;
        });
        pendingMessagesRef.current.set(tempId, { chatId, text });
        if (stompClientRef.current?.connected) {
            stompClientRef.current.send(`/app/chats/${chatId}/messages/send`, {}, JSON.stringify({
                tempId: tempId,
                text: text
            }));
        } else {
            console.warn('Нет соединения WebSocket, сообщение не отправлено');
            setMessages(prev => {
                const current = prev[chatIdStr] || [];
                const updated = current.map(m => m.id === tempId ? { ...m, isPending: false, text: m.text + ' (не отправлено)' } : m);
                return { ...prev, [chatIdStr]: updated };
            });
            pendingMessagesRef.current.delete(tempId);
        }
    };

    const markMessagesRead = async (chatId, upToMessageId) => {
        if (!stompClientRef.current?.connected) return;
        const chatIdStr = String(chatId);
        updateChatInList(chatIdStr, (chat) => ({ ...chat, unreadCount: 0 }));
        console.log('markMessagesRead selectedChatIdRef.current type:', typeof selectedChatIdRef.current, 'value:', selectedChatIdRef.current);
        console.log('markMessagesRead chatIdStr type:', typeof chatIdStr, 'value:', chatIdStr);
        if (selectedChatIdRef.current === chatIdStr) {
            setMessages(prev => {
                const current = prev[chatIdStr] || [];
                const updated = current.map(msg => ({
                    ...msg,
                    isReadByMe: msg.id <= String(upToMessageId) ? true : (msg.isReadByMe || false)
                }));
                return { ...prev, [chatIdStr]: updated };
            });
        }
        stompClientRef.current.send(`/app/chats/${chatId}/messages/${upToMessageId}/up-to-read`, {}, {});
    };


    React.useEffect(() => {
        const init = async () => {
            if (!AuthService.isAuthenticated()) {
                window.location.href = getFormsPath('/');
                return;
            }
            try {
                const profile = await API.getMyProfile();
                if (profile.success) {
                    setUser(profile.data);
                } else {
                    AuthService.logout();
                }
            } catch(e) {
                setError('Ошибка загрузки профиля');
            }
        };
        init();
    }, []);

    React.useEffect(() => {
        if (user && !isInitialized.current) {
            isInitialized.current = true;
            loadChats();
        }
        return () => {
            if (stompClientRef.current?.connected) {
                subscriptionsRef.current.forEach(sub => sub.unsubscribe());
                stompClientRef.current.disconnect();
            }
        };
    }, [user]);

    const handleSelectChat = (chatId) => {
        const chatIdStr = String(chatId);
        selectedChatIdRef.current = chatIdStr;  // сохраняем в ref
        setSelectedChatId(chatIdStr);
        if (messages[chatIdStr] === undefined) {
            loadMessages(chatIdStr, null, 'BACKWARD');
        }
        const chat = chats.find(c => c.id === chatIdStr);
        const lastMsgId = chat?.lastMessage?.id;
        const unreadCount = chat?.unreadCount || 0;
        // Отправляем только если есть непрочитанные сообщения
        if (lastMsgId && unreadCount > 0) {
            markMessagesRead(chatIdStr, lastMsgId);
        }
    };
    const handleToggleMenu = () => setIsMenuOpen(!isMenuOpen);
    const handleLogout = () => AuthService.logout();
    const handleProfile = () => alert('Профиль (будет реализовано)');
    const handleTogglePin = async (chatId, currentPinned) => {
        try {
            const res = await API.updateSelfChatSettings(chatId, !currentPinned);
            if (!res.success) throw new Error(res.error);
            updateChatInList(chatId, (chat) => ({ ...chat, isPinned: !currentPinned }));
        } catch (err) {
            alert('Не удалось закрепить чат');
        }
    };

    if (!user) {
        return <div className="main-area">Загрузка...</div>;
    }

    return (
        <div className="messenger-container">
            <Sidebar
                user={user}
                chats={chats}
                selectedChatId={selectedChatId}
                onSelectChat={handleSelectChat}
                isLoadingChats={isLoadingChats}
                isMenuOpen={isMenuOpen}
                onToggleMenu={handleToggleMenu}
                onLogout={handleLogout}
                onProfile={handleProfile}
                onTogglePin={handleTogglePin}
            />
            <ChatWindow
                chatId={selectedChatId}
                user={user}
                messages={messages[selectedChatId] || []}
                isLoading={isLoadingMessages}
                hasMore={hasMoreMessages[selectedChatId] || false}
                onLoadMore={() => loadMoreMessages(selectedChatId)}
                onSendMessage={sendMessage}
                onMarkRead={markMessagesRead}
            />
            {error && <div style={{position:'fixed', bottom:20, left:20, background:'#e74c3c', color:'white', padding:10, borderRadius:8, zIndex:1000}}>{error}</div>}
        </div>
    );
};

ReactDOM.render(<MessengerApp />, document.getElementById('root'));