/**
 * Компонент списка чатов
 */
const ChatList = ({ chats, selectedChatId, onSelectChat, isLoading, onLoadMore, hasMoreChats }) => {
    const scrollContainerRef = React.useRef(null);

    // Получить инициалы из названия чата
    const getInitials = (name) => {
        if (!name) name = '';
        return name.split(' ').map(word => word[0]).join('').toUpperCase().slice(0, 2);
    };

    // Форматировать превью сообщения
    const formatMessagePreview = (message) => {
        if (!message) return 'Нет сообщений';
        return message.text || '📎 Файл';
    };

    // Обработчик скролла для бесконечной прокрутки
    const handleScroll = React.useCallback((e) => {
        if (!hasMoreChats || !onLoadMore) return;

        const target = e.target;
        const scrolledToBottom = target.scrollTop + target.clientHeight >= target.scrollHeight - 50;

        if (scrolledToBottom && !isLoading) {
            console.log('[ChatList] Scrolled to bottom, loading more chats');
            onLoadMore();
        }
    }, [hasMoreChats, onLoadMore, isLoading]);

    if (isLoading && (!Array.isArray(chats) || chats.length === 0)) {
        return (
            <div className="chats-list" ref={scrollContainerRef} onScroll={handleScroll}>
                <div className="loading-spinner">
                    <div>
                        <i className="bi bi-hourglass-split"></i>
                        <p style={{ marginTop: '8px' }}>Загрузка чатов...</p>
                    </div>
                </div>
            </div>
        );
    }

    if (!Array.isArray(chats) || chats.length === 0) {
        return (
            <div className="chats-list" ref={scrollContainerRef} onScroll={handleScroll}>
                <div className="empty-state">
                    <i className="bi bi-chat-left"></i>
                    <p>Нет чатов</p>
                </div>
            </div>
        );
    }

    return (
        <div className="chats-list" ref={scrollContainerRef} onScroll={handleScroll}>
            {chats.map(chat => (
                <div
                    key={chat.id}
                    className={`chat-item ${selectedChatId === chat.id ? 'active' : ''}`}
                    onClick={() => onSelectChat(chat.id)}
                >
                    <div className="chat-avatar">
                        {getInitials(chat.name)}
                    </div>
                    <div className="chat-info">
                        <div className="chat-name">{chat.name}</div>
                        <div className="chat-preview">
                            {formatMessagePreview(chat.lastMessage)}
                        </div>
                    </div>
                    {chat.unreadCount > 0 && (
                        <div className="chat-unread-badge">
                            {chat.unreadCount > 99 ? '99+' : chat.unreadCount}
                        </div>
                    )}
                </div>
            ))}
            {isLoading && chats.length > 0 && (
                <div style={{ padding: '20px', textAlign: 'center', color: 'var(--text-light)' }}>
                    <i className="bi bi-hourglass-split"></i>
                    <p style={{ marginTop: '8px', fontSize: '12px' }}>Загрузка еще чатов...</p>
                </div>
            )}
        </div>
    );
};

