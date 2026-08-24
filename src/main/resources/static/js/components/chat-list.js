/**
 * Компонент списка чатов
 */
const ChatList = ({ chats, selectedChatId, onSelectChat, isLoading, onTogglePin, user }) => {
    const getInitials = (name) => (name || '').split(' ').map(word => word[0]).join('').toUpperCase().slice(0, 2);

    const formatLastMessageTime = (sentAt) => {
        if (!sentAt) return '';
        const date = new Date(sentAt);
        const now = new Date();
        const diffMs = now - date;
        const oneDay = 24 * 60 * 60 * 1000;

        // Сегодня — время ЧЧ:ММ
        if (date.toDateString() === now.toDateString()) {
            return date.toLocaleTimeString('ru-RU', { hour: '2-digit', minute: '2-digit' });
        }
        // Вчера
        const yesterday = new Date(now.getTime() - oneDay);
        if (date.toDateString() === yesterday.toDateString()) {
            return 'Вчера';
        }
        // На этой неделе — день недели
        if (diffMs < 7 * oneDay) {
            return date.toLocaleDateString('ru-RU', { weekday: 'short' });
        }
        // Иначе — дата ДД.ММ
        return date.toLocaleDateString('ru-RU', { day: '2-digit', month: '2-digit' });
    };

    if (isLoading && (!Array.isArray(chats) || chats.length === 0)) {
        return <div className="chats-list"><div className="loading-spinner"><i className="bi bi-hourglass-split"></i><p>Загрузка чатов...</p></div></div>;
    }

    if (!Array.isArray(chats) || chats.length === 0) {
        return <div className="chats-list"><div className="empty-state"><i className="bi bi-chat-left"></i><p>Нет чатов</p></div></div>;
    }

    return (
        <div className="chats-list">
            {chats.map(chat => (
                <div key={chat.id.toString()} className={`chat-item ${selectedChatId === chat.id.toString() ? 'active' : ''}`} onClick={() => onSelectChat(chat.id)}>
                    <div className="chat-avatar">{getInitials(chat.name)}</div>
                    <div className="chat-info">
                        <div className="chat-name-row">
                            <div className="chat-name">{chat.name}</div>
                            {chat.lastMessage && chat.lastMessage.sentAt ? (
                                <div className="chat-time">{formatLastMessageTime(chat.lastMessage.sentAt)}</div>
                            ) : null}
                        </div>
                        <div className="chat-preview">
                            {chat.lastMessage
                                ? (chat.lastMessage.senderId === user.id ? 'Вы: ' : '') + (chat.lastMessage.text || '')
                                : 'Нет сообщений'}
                        </div>
                    </div>
                    {Number(chat.unreadCount) > 0 && <div className="chat-unread-badge">{Number(chat.unreadCount) > 99 ? '99+' : Number(chat.unreadCount)}</div>}
                    <button className="sidebar-btn" style={{ marginLeft: 'auto' }} onClick={(e) => { e.stopPropagation(); onTogglePin?.(chat.id, chat.isPinned); }} title={chat.isPinned ? 'Открепить' : 'Закрепить'}>
                        <i className={`bi ${chat.isPinned ? 'bi-pin-fill' : 'bi-pin'}`}></i>
                    </button>
                </div>
            ))}
        </div>
    );
};