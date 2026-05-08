/**
 * Компонент окна чата (список сообщений + форма отправки)
 */
const ChatWindow = ({ chatId, user, messages, isLoading, hasMore, onLoadMore, onSendMessage, onMarkRead }) => {
    const [newMessageText, setNewMessageText] = React.useState('');
    const [isSending, setIsSending] = React.useState(false);
    const messagesEndRef = React.useRef(null);
    const messagesContainerRef = React.useRef(null);
    const prevChatIdRef = React.useRef(null);

    // Автоматический скролл вниз при новых сообщениях
    React.useEffect(() => {
        if (messagesContainerRef.current && messages.length > 0) {
            const isNearBottom = messagesContainerRef.current.scrollHeight - messagesContainerRef.current.scrollTop - messagesContainerRef.current.clientHeight < 100;
            if (isNearBottom || prevChatIdRef.current !== chatId) {
                messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
            }
        }
        prevChatIdRef.current = chatId;
    }, [messages, chatId]);

    // Обработчик скролла для подгрузки истории
    const handleScroll = React.useCallback(() => {
        if (!messagesContainerRef.current || isLoading || !hasMore) return;
        const { scrollTop } = messagesContainerRef.current;
        if (scrollTop === 0) {
            onLoadMore();
        }
    }, [isLoading, hasMore, onLoadMore]);

    React.useEffect(() => {
        const container = messagesContainerRef.current;
        if (container) {
            container.addEventListener('scroll', handleScroll);
            return () => container.removeEventListener('scroll', handleScroll);
        }
    }, [handleScroll]);

    const handleSubmit = async (e) => {
        e.preventDefault();
        if (!newMessageText.trim() || isSending) return;
        setIsSending(true);
        await onSendMessage(chatId, newMessageText.trim());
        setNewMessageText('');
        setIsSending(false);
    };

    const getMessageStatusIcon = (msg) => {
        if (msg.isPending) return <i className="bi bi-clock-history"></i>;
        if (!msg.isDelivered) return <i className="bi bi-check"></i>;
        if (msg.isReadByMe) return <i className="bi bi-check2-all"></i>;
        return <i className="bi bi-check"></i>;
    };

    const formatTime = (isoString) => {
        if (!isoString) return '';
        const date = new Date(isoString);
        return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    };

    const getInitials = (name) => (name || '').split(' ').map(w => w[0]).join('').toUpperCase().slice(0, 2);

    return (
        <div className="chat-window">
            {!chatId ? (
                <div className="main-area">
                    <div className="placeholder-icon"><i className="bi bi-chat-dots"></i></div>
                    <div className="placeholder-text">Нажмите на чат, чтобы начать общение!</div>
                    <div className="placeholder-subtext">Выберите чат из списка слева</div>
                </div>
            ) : (
                <>
                    <div className="chat-messages" ref={messagesContainerRef}>
                        {isLoading && messages.length === 0 && (
                            <div className="loading-spinner"><i className="bi bi-hourglass-split"></i><p>Загрузка сообщений...</p></div>
                        )}
                        {messages.map((msg, idx) => (
                            <div key={msg.id || msg.tempId} className={`message-item ${msg.senderId === user.id ? 'my-message' : 'other-message'} ${msg.isPending ? 'pending' : ''}`}>
                                {msg.senderId !== user.id && <div className="message-avatar">{getInitials(msg.senderName || 'U')}</div>}
                                <div className="message-bubble">
                                    <div className="message-text">{msg.text}</div>
                                    <div className="message-meta">
                                        <span className="message-time">{formatTime(msg.sentAt)}</span>
                                        {msg.senderId === user.id && (
                                            <span className="message-status">{getMessageStatusIcon(msg)}</span>
                                        )}
                                    </div>
                                </div>
                            </div>
                        ))}
                        <div ref={messagesEndRef} />
                    </div>
                    <form className="chat-input-form" onSubmit={handleSubmit}>
                        <input
                            type="text"
                            className="chat-input"
                            placeholder="Введите сообщение..."
                            value={newMessageText}
                            onChange={(e) => setNewMessageText(e.target.value)}
                            disabled={isSending}
                        />
                        <button type="submit" className="send-btn" disabled={isSending || !newMessageText.trim()}>
                            <i className="bi bi-send-fill"></i>
                        </button>
                    </form>
                </>
            )}
        </div>
    );
};