/**
 * Главный компонент приложения мессенджера
 */
const MessengerApp = () => {
    const [user, setUser] = React.useState(null);
    const [chats, setChats] = React.useState([]);
    const [selectedChatId, setSelectedChatId] = React.useState(null);
    const [isLoadingChats, setIsLoadingChats] = React.useState(true);
    const [isMenuOpen, setIsMenuOpen] = React.useState(false);
    const [error, setError] = React.useState(null);
    const stompClientRef = React.useRef(null);
    const subscriptionsRef = React.useRef(new Map());

    const updateChatInList = (chatId, updater) => {
        setChats(prevChats => {
            console.log('[updateChatInList] prevChats length:', prevChats.length);
            const index = prevChats.findIndex(c => c.id === chatId);
            console.log('[updateChatInList] chatId:', chatId, 'index:', index);
            if (index === -1) {
                console.warn('[updateChatInList] Chat not found, returning unchanged');
                return prevChats;
            }
            const updated = updater(prevChats[index]);
            console.log('[updateChatInList] updated chat:', updated);
            const newChats = [...prevChats];
            newChats[index] = updated;
            newChats.sort((a, b) => {
                if (a.isPinned !== b.isPinned) return a.isPinned ? -1 : 1;
                // Используем Number для преобразования строковых ID, но если lastMessage нет – ставим 0
                const lastA = a.lastMessage?.id ? Number(a.lastMessage.id) : 0;
                const lastB = b.lastMessage?.id ? Number(b.lastMessage.id) : 0;
                return lastB - lastA;
            });
            console.log('[updateChatInList] after update:', newChats.map(c => ({ id: c.id, isPinned: c.isPinned, lastId: c.lastMessage?.id })));
            return newChats;
        });
    };

    const removeChatFromList = (chatId) => {
        console.log('[removeChatFromList] removing chatId:', chatId);
        setChats(prev => {
            const newChats = prev.filter(c => c.id !== chatId);
            console.log('[removeChatFromList] prev length:', prev.length, 'new length:', newChats.length);
            return newChats;
        });
    };

    const addChatToList = (newChat) => {
        setChats(prev => {
            if (prev.some(c => c.id === newChat.id)) return prev;
            return [newChat, ...prev];
        });
    };

    const subscribeToChatTopics = (chatIds) => {
        if (!stompClientRef.current?.connected) return;
        chatIds.forEach(chatId => {
            if (subscriptionsRef.current.has(chatId)) return;
            const sub = stompClientRef.current.subscribe(`/topic/chats/${chatId}`, (message) => {
                try {
                    const body = JSON.parse(message.body);
                    const receivedChatId = String(body.chatId); // Приводим к строке
                    if (body.messageId != null && receivedChatId === chatId) {
                        console.log('[WS] New message in chat', chatId, body);
                        updateChatInList(chatId, (chat) => ({
                            ...chat,
                            lastMessage: {
                                id: String(body.messageId),
                                text: body.text,
                                senderId: String(body.senderId),
                                sentAt: body.sentAt
                            },
                            unreadCount: (Number(chat.unreadCount) || 0) + 1
                        }));
                    }
                    if (body.deletedAt != null && receivedChatId === chatId) {
                        removeChatFromList(chatId);
                    }
                } catch (e) { console.error('Error parsing topic message', e); }
            });
            subscriptionsRef.current.set(chatId, sub);
        });
    };

    const unsubscribeFromAllTopics = () => {
        subscriptionsRef.current.forEach(sub => sub.unsubscribe());
        subscriptionsRef.current.clear();
    };

    const loadChats = async () => {
        console.log('[loadChats] started');
        setIsLoadingChats(true);
        try {
            const metaRes = await API.getChatsMeta();
            console.log('[loadChats] metaRes:', metaRes);
            if (!metaRes.success) throw new Error(metaRes.error);
            const metaList = metaRes.data || [];
            if (metaList.length === 0) {
                setChats([]);
                return;
            }
            const chatIds = metaList.map(m => m.id);
            console.log('[loadChats] chatIds:', chatIds);
            const batchRes = await API.getChatsBatch(chatIds);
            console.log('[loadChats] batchRes:', batchRes);
            if (!batchRes.success) throw new Error(batchRes.error);
            let fullChats = batchRes.data || [];

            fullChats = fullChats.map(chat => {
                const meta = metaList.find(m => m.id === chat.id);
                return {
                    ...chat,
                    isPinned: meta?.isPinned || false,
                    unreadCount: Number(meta?.unreadCount) || 0
                };
            });

            fullChats.sort((a, b) => {
                if (a.isPinned !== b.isPinned) return a.isPinned ? -1 : 1;
                const lastA = a.lastMessage?.id ? Number(a.lastMessage.id) : 0;
                const lastB = b.lastMessage?.id ? Number(b.lastMessage.id) : 0;
                return lastB - lastA;
            });

            console.log('[loadChats] fullChats count:', fullChats.length);
            setChats(fullChats);
            if (stompClientRef.current?.connected) {
                subscribeToChatTopics(chatIds);
            }
        } catch (err) {
            console.error('[loadChats] error:', err);
            setError(err.message);
        } finally {
            setIsLoadingChats(false);
        }
    };

    const initWebSocket = () => {
        const token = AuthService.getToken();
        if (!token) {
            console.warn('[WS] No token, skipping');
            return;
        }
        const socketUrl = `${window.location.protocol}//${window.location.host}${getBasePath()}/ws?token=${token}`;
        console.log('[WS] Connecting to', socketUrl);
        const socket = new SockJS(socketUrl);
        const stompClient = Stomp.over(socket);
        stompClient.debug = (str) => console.log('[STOMP]', str);
        stompClient.connect({}, (frame) => {
            console.log('[WS] Connected');
            stompClientRef.current = stompClient;
            stompClient.subscribe('/user/queue/chats', (msg) => {
                try {
                    const newChat = JSON.parse(msg.body);
                    console.log('[WS] New chat:', newChat);
                    if (newChat.id) {
                        const chatWithStrings = {
                            ...newChat,
                            id: String(newChat.id),
                            isPinned: newChat.isPinned || false,
                            unreadCount: Number(newChat.unreadCount) || 0
                        };
                        addChatToList(chatWithStrings);
                        subscribeToChatTopics([chatWithStrings.id]);
                    }
                } catch(e) { console.error(e); }
            });
            stompClient.subscribe('/user/queue/chat-settings', (msg) => {
                try {
                    const update = JSON.parse(msg.body);
                    const chatId = String(update.chatId);
                    console.log('[WS] Settings update:', update);
                    if (chatId && typeof update.isPinned != null) {
                        updateChatInList(chatId , (chat) => ({ ...chat, isPinned: update.isPinned }));
                    }
                } catch(e) { console.error(e); }
            });
            if (chats.length) subscribeToChatTopics(chats.map(c => c.id));
        }, (err) => {
            console.error('[WS] Connection error', err);
        });
    };

    React.useEffect(() => {
        const init = async () => {
            if (!AuthService.isAuthenticated()) {
                window.location.href = getFormsPath('/');
                return;
            }
            try {
                const profile = await API.getMyProfile();
                console.log('[App] Profile response:', profile);
                if (profile.success) {
                    setUser(profile.data);
                } else {
                    console.error('[App] Profile error:', profile.error);
                    AuthService.logout();
                }
            } catch(e) {
                console.error('[App] Profile exception:', e);
                setError('Ошибка загрузки профиля');
            }
        };
        init();
    }, []);

    React.useEffect(() => {
        if (user) {
            console.log('[App] User loaded, loading chats...');
            loadChats();
            initWebSocket();
        }
        return () => {
            if (stompClientRef.current?.connected) {
                unsubscribeFromAllTopics();
                stompClientRef.current.disconnect();
            }
        };
    }, [user]);

    const handleSelectChat = (chatId) => setSelectedChatId(chatId);
    const handleToggleMenu = () => setIsMenuOpen(!isMenuOpen);
    const handleLogout = () => AuthService.logout();
    const handleProfile = () => alert('Профиль');
    const handleTogglePin = async (chatId, currentPinned) => {
        try {
            const res = await API.updateSelfChatSettings(chatId, !currentPinned);
            if (!res.success) throw new Error(res.error);
            updateChatInList(chatId, (chat) => ({ ...chat, isPinned: !currentPinned }));
        } catch (err) {
            console.error('Toggle pin error', err);
            alert('Не удалось закрепить');
        }
    };

    if (!user) {
        return (
            <div className="main-area">
                <div>Загрузка...</div>
                {error && <div style={{ color: 'red', marginTop: '10px' }}>{error}</div>}
            </div>
        );
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
            <MainArea selectedChatId={selectedChatId} />
            {error && (
                <div style={{ position: 'fixed', bottom: '20px', left: '20px', background: '#e74c3c', color: 'white', padding: '10px', borderRadius: '8px', zIndex: 9999 }}>
                    {error}
                </div>
            )}
        </div>
    );
};

ReactDOM.render(<MessengerApp />, document.getElementById('root'));