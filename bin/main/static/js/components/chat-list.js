/**
 * Компонент списка чатов
 */
const ChatList = ({ chats, selectedChatId, onSelectChat, isLoading, onLoadMore, hasMoreChats, onTogglePin }) => {
    const scrollContainerRef = React.useRef(null);

    const getInitials = (name) => (name || '').split(' ').map(word => word[0]).join('').toUpperCase().slice(0, 2);
    
    const formatMessagePreview = (message) => message?.text || '-_-';

    const handleScroll = React.useCallback((e) => {
        if (!hasMoreChats || !onLoadMore) return;
        const target = e.target;
        if (target.scrollTop + target.clientHeight >= target.scrollHeight - 50 && !isLoading) onLoadMore();
    }, [hasMoreChats, onLoadMore, isLoading]);

    if (isLoading && (!Array.isArray(chats) || chats.length === 0)) {
        return <div className="chats-list" ref={scrollContainerRef} onScroll={handleScroll}><div className="loading-spinner"><div><i className="bi bi-hourglass-split"></i><p>Загрузка чатов...</p></div></div></div>;
    }

    if (!Array.isArray(chats) || chats.length === 0) {
        return <div className="chats-list" ref={scrollContainerRef} onScroll={handleScroll}><div className="empty-state"><i className="bi bi-chat-left"></i><p>Нет чатов</p></div></div>;
    }

    console.log('[ChatList] render, chats length:', chats?.length);

    return (
        <div className="chats-list" ref={scrollContainerRef} onScroll={handleScroll}>
            {chats.map(chat => (
                <div key={chat.id} className={`chat-item ${selectedChatId === chat.id ? 'active' : ''}`} onClick={() => onSelectChat(chat.id)}>
                    <div className="chat-avatar">{getInitials(chat.name)}</div>
                    <div className="chat-info">
                        <div className="chat-name">{chat.name}</div>
                        <div className="chat-preview">{formatMessagePreview(chat.lastMessage)}</div>
                    </div>
                    {Number(chat.unreadCount) > 0 && <div className="chat-unread-badge">{Number(chat.unreadCount) > 99 ? '99+' : Number(chat.unreadCount)}</div>}
                    <button className="sidebar-btn" style={{ marginLeft: 'auto' }} onClick={(e) => { e.stopPropagation(); onTogglePin?.(chat.id, chat.isPinned); }} title={chat.isPinned ? 'Открепить' : 'Закрепить'}>
                        <i className={`bi ${chat.isPinned ? 'bi-pin-fill' : 'bi-pin'}`}></i>
                    </button>
                </div>
            ))}
            {isLoading && chats.length > 0 && <div className="loading-more">Загрузка ещё...</div>}
        </div>
    );
};