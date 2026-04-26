/**
 * Главный компонент приложения мессенджера
 */
const MessengerApp = () => {
    const [user, setUser] = React.useState(null);
    const [chats, setChats] = React.useState([]); // Теперь это массив вместо объекта
    const [allChatIds, setAllChatIds] = React.useState([]); // Все ID чатов пользователя
    const [selectedChatId, setSelectedChatId] = React.useState(null);
    const [isLoadingChats, setIsLoadingChats] = React.useState(true);
    const [isMenuOpen, setIsMenuOpen] = React.useState(false);
    const [error, setError] = React.useState(null);
    const [hasMoreChats, setHasMoreChats] = React.useState(false);
    const [nextCursor, setNextCursor] = React.useState(null);
    const wsRef = React.useRef(null);
    const subscribedChatsRef = React.useRef(new Set());

    // Проверить авторизацию при загрузке
    React.useEffect(() => {
        const checkAuth = async () => {
            console.log('[Messenger] Checking authentication...');

            if (!AuthService.isAuthenticated()) {
                console.log('[Messenger] Not authenticated, redirecting to /');
                window.location.href = getFormsPath('/');
                return;
            }

            console.log('[Messenger] Loading user profile...');

            try {
                // Загружаем профиль пользователя
                const result = await API.getMyProfile();
                console.log('[Messenger] Profile result:', result);

                if (result.success) {
                    console.log('[Messenger] Profile loaded:', result.data);
                    setUser(result.data);
                } else {
                    console.error('[Messenger] Profile loading failed:', result.error);
                    AuthService.logout();
                }
            } catch (error) {
                console.error('[Messenger] Error loading profile:', error);
                setError('Ошибка загрузки профиля');
            }
        };

        checkAuth();
    }, []);

    // Получить все chatIds и загрузить первую партию чатов
    React.useEffect(() => {
        if (!user) return;

        const loadChatIds = async () => {
            try {
                // Получаем все ID чатов
                const result = await API.getUserChatIds();
                if (result.success && result.data) {
                    const chatIds = result.data; // Это массив ID
                    setAllChatIds(chatIds);
                    console.log('[Messenger] Got all chat IDs:', chatIds.length);

                    // Подписываемся на WebSocket топики для каждого чата
                    subscribeToChats(chatIds);

                    // Загружаем первую партию чатов (пагинация)
                    await loadChatsPage(null);
                } else {
                    console.error('[Messenger] Failed to get chat IDs:', result.error);
                    setError(result.error || 'Ошибка получения чатов');
                }
            } catch (error) {
                console.error('[Messenger] Error loading chat IDs:', error);
                setError('Ошибка подключения');
            }
        };

        loadChatIds();
    }, [user]);

    // Функция для подписки на WebSocket топики чатов
    const subscribeToChats = (chatIds) => {
        console.log('[Messenger] Subscribing to chat topics:', chatIds.length);
        // Используем client из stomp, предполагая что он инициализирован в другом месте
        // Пока это заглушка, логика WebSocket будет в компоненте
    };

    // Загрузить страницу чатов с пагинацией
    const loadChatsPage = async (cursor) => {
        if (cursor === undefined) {
            setIsLoadingChats(true);
        }

        try {
            const result = await API.getUserChats(null, null, cursor, 20);
            console.log('[Messenger] Chats page result:', result);

            if (result.success && result.data) {
                const pageChats = result.data.chats || [];

                if (cursor === null || cursor === undefined) {
                    // Первая загрузка
                    setChats(pageChats);
                } else {
                    // Добавляем к существующему списку
                    setChats(prev => [...prev, ...pageChats]);
                }

                setNextCursor(result.data.nextCursor);
                setHasMoreChats(result.data.nextCursor !== null);
                setError(null);
            } else {
                setError(result.error || 'Ошибка загрузки чатов');
            }
        } catch (error) {
            console.error('[Messenger] Error loading chats page:', error);
            setError('Ошибка загрузки чатов');
        } finally {
            setIsLoadingChats(false);
        }
    };

    // Загрузить еще чаты при скролле вниз
    const handleLoadMore = () => {
        if (hasMoreChats && nextCursor !== null && !isLoadingChats) {
            loadChatsPage(nextCursor);
        }
    };

    const handleSelectChat = (chatId) => {
        setSelectedChatId(chatId);
    };

    const handleToggleMenu = () => {
        setIsMenuOpen(!isMenuOpen);
    };

    const handleLogout = () => {
        AuthService.logout();
    };

    const handleProfile = () => {
        setIsMenuOpen(false);
        // Переход на профиль позже
        alert('Редактирование профиля будет добавлено позже');
    };

    // Обновить чат в списке (переместить вверх)
    const updateChatInList = (chatId, updatedChatData) => {
        setChats(prevChats => {
            // Находим и удаляем чат из списка
            const filteredChats = prevChats.filter(chat => chat.id !== chatId);

            // Если чат найден, добавляем обновленный в начало
            if (filteredChats.length < prevChats.length) {
                const chatToMove = prevChats.find(chat => chat.id === chatId);
                if (chatToMove) {
                    return [{ ...chatToMove, ...updatedChatData }, ...filteredChats];
                }
            }

            return prevChats;
        });
    };

    // Закрывать меню при клике вне его
    React.useEffect(() => {
        const handleClickOutside = (e) => {
            if (isMenuOpen && !e.target.closest('.sidebar-footer')) {
                setIsMenuOpen(false);
            }
        };

        document.addEventListener('click', handleClickOutside);
        return () => document.removeEventListener('click', handleClickOutside);
    }, [isMenuOpen]);

    if (!user) {
        return (
            <div className="main-area">
                <div className="placeholder-icon">
                    <i className="bi bi-hourglass-split"></i>
                </div>
                <div className="placeholder-text">Загрузка...</div>
                {error && <div className="placeholder-subtext" style={{ color: '#e74c3c', marginTop: '16px' }}>{error}</div>}
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
                onLoadMore={handleLoadMore}
                hasMoreChats={hasMoreChats}
            />
            <MainArea
                selectedChatId={selectedChatId}
                onChatUpdated={(chatId, data) => updateChatInList(chatId, data)}
                allChatIds={allChatIds}
            />
        </div>
    );
};

// Монтировать приложение
ReactDOM.render(<MessengerApp />, document.getElementById('root'));