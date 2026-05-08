/**
 * Компонент списка чатов
 */
const ChatList = ({ chats, selectedChatId, onSelectChat, isLoading, onTogglePin }) => {
    const getInitials = (name) => (name || '').split(' ').map(word => word[0]).join('').toUpperCase().slice(0, 2);
    const formatMessagePreview = (message) => message?.text || '-_-';

    if (isLoading && (!Array.isArray(chats) || chats.length === 0)) {
        return <div className="chats-list"><div className="loading-spinner"><i className="bi bi-hourglass-split"></i><p>Загрузка чатов...</p></div></div>;
    }

    if (!Array.isArray(chats) || chats.length === 0) {
        return <div className="chats-list"><div className="empty-state"><i className="bi bi-chat-left"></i><p>Нет чатов</p></div></div>;
    }

    return (
        <div className="chats-list">
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
        </div>
    );
};