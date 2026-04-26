/**
 * Компонент основной области (справа)
 */
const MainArea = ({ selectedChatId, onChatUpdated, allChatIds }) => {
    const [wsClient, setWsClient] = React.useState(null);
    const subscribedChatsRef = React.useRef(new Set());

    // Инициализация WebSocket и подписка на топики чатов
    React.useEffect(() => {
        if (!allChatIds || allChatIds.length === 0) return;

        const initWebSocket = () => {
            console.log('[MainArea] Initializing WebSocket for chat subscriptions');

            // Создаем STOMP клиент (используем SockJS для WebSocket)
            const socket = new SockJS(getApiPath('/ws'));
            const stompClient = Stomp.over(socket);

            stompClient.connect({}, (frame) => {
                console.log('[MainArea] WebSocket Connected:', frame);

                // Подписываемся на топики всех чатов пользователя
                allChatIds.forEach(chatId => {
                    if (!subscribedChatsRef.current.has(chatId)) {
                        const subscription = stompClient.subscribe(
                            `/user/queue/chats/${chatId}`,
                            (message) => {
                                console.log(`[MainArea] Message received for chat ${chatId}:`, message);
                                try {
                                    const body = JSON.parse(message.body);
                                    handleChatUpdate(chatId, body);
                                } catch (e) {
                                    console.error('[MainArea] Error parsing WebSocket message:', e);
                                }
                            }
                        );
                        subscribedChatsRef.current.add(chatId);
                        console.log(`[MainArea] Subscribed to chat ${chatId}`);
                    }
                });

                setWsClient(stompClient);
            }, (error) => {
                console.error('[MainArea] WebSocket connection error:', error);
            });
        };

        initWebSocket();

        return () => {
            if (wsClient) {
                // При размонтировании компонента отключиться от WebSocket
                // wsClient.disconnect(() => console.log('[MainArea] WebSocket disconnected'));
            }
        };
    }, [allChatIds]);

    // Обработчик обновлений чата (новые сообщения, обновления и т.д.)
    const handleChatUpdate = (chatId, data) => {
        console.log('[MainArea] Processing chat update for chat', chatId, data);

        // Проверяем тип обновления
        if (data.type === 'MessageNewResponse' ||
            data.type === 'message_new' ||
            data.messageId !== undefined) {
            // Новое сообщение - перемещаем чат вверх
            console.log('[MainArea] New message in chat', chatId);
            if (onChatUpdated) {
                onChatUpdated(chatId, {
                    lastMessage: {
                        text: data.text || 'Новое сообщение',
                        senderId: data.senderId
                    }
                });
            }
        } else if (data.type === 'SelfChatSettingsUpdateResponse' ||
                   data.isPinned !== undefined) {
            // Обновление настроек чата (закреплен ли)
            console.log('[MainArea] Chat settings updated for', chatId, data);
            if (onChatUpdated && data.isPinned) {
                // Если чат был закреплен, перемещаем его вверх
                onChatUpdated(chatId, { isPinned: data.isPinned });
            }
        }
        // Для остальных действий (если они не меняют отображение) - игнорируем
    };

    if (!selectedChatId) {
        return (
            <div className="main-area">
                <div className="placeholder-icon">
                    <i className="bi bi-chat-dots"></i>
                </div>
                <div className="placeholder-text">Нажмите на чат, чтобы начать общение!</div>
                <div className="placeholder-subtext">Выберите чат из списка слева</div>
            </div>
        );
    }

    return (
        <div className="main-area">
            <div className="placeholder-icon">
                <i className="bi bi-hourglass-split"></i>
            </div>
            <div className="placeholder-text">Загрузка чата...</div>
            <div className="placeholder-subtext">Экран чата будет добавлен позже</div>
        </div>
    );
};