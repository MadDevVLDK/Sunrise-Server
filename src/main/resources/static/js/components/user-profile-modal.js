const UserProfileModal = ({ isOpen, userId, onClose, onSendMessage }) => {
    const [profile, setProfile] = React.useState(null);
    const [isLoading, setIsLoading] = React.useState(false);
    const [isSending, setIsSending] = React.useState(false);

    React.useEffect(() => {
        if (isOpen && userId) {
            setProfile(null);
            loadProfile();
        }
    }, [isOpen, userId]);

    const loadProfile = async () => {
        setIsLoading(true);
        try {
            const result = await API.getOtherProfile(userId.toString());
            setProfile(result);
        } catch (e) {
            if (typeof ApiError !== 'undefined' && e instanceof ApiError) {
                Toast.error(e.displayMessage);
            } else {
                Toast.error('Ошибка загрузки профиля');
            }
        } finally {
            setIsLoading(false);
        }
    };

    const handleSendMessage = async () => {
        if (isSending || !onSendMessage) return;
        setIsSending(true);
        try {
            await onSendMessage(userId);
        } finally {
            setIsSending(false);
        }
    };

    if (!isOpen || !userId) return null;

    const getInitials = (name) => {
        if (!name) return 'П';
        return name.split(' ').map(function(w) { return w[0]; }).join('').toUpperCase().slice(0, 2);
    };

    const displayName = profile ? (profile.name || 'Пользователь') : '';
    const displayUsername = profile ? (profile.username || '') : '';

    return (
        <div className="modal-overlay" onClick={onClose}>
            <div className="chat-info-modal" onClick={function(e) { e.stopPropagation(); }}>
                <div className="chat-info-header">
                    <h3>Профиль пользователя</h3>
                    <button className="chat-info-close" onClick={onClose}>
                        <i className="bi bi-x-lg"></i>
                    </button>
                </div>

                {isLoading ? (
                    <div className="chat-info-loading" style={{ padding: '40px 24px' }}>
                        <div className="sunrise-toast-spinner"></div>
                        <span>Загрузка профиля...</span>
                    </div>
                ) : profile ? (
                    <React.Fragment>
                        <div className="chat-info-profile">
                            <div className="chat-info-avatar">{getInitials(displayName)}</div>
                            <div className="chat-info-name">{displayName}</div>
                            <div className="chat-info-subtitle">
                                {displayUsername ? '@' + displayUsername : ''}
                            </div>
                        </div>

                        <div className="chat-info-content">
                            <div className="chat-info-details">
                                <div className="chat-info-row">
                                    <i className="bi bi-calendar"></i>
                                    <span>Зарегистрирован</span>
                                    <span className="chat-info-value">
                                        {profile.createdAt ? new Date(profile.createdAt).toLocaleDateString('ru-RU') : '—'}
                                    </span>
                                </div>
                                <div className="chat-info-row">
                                    <i className="bi bi-arrow-repeat"></i>
                                    <span>Профиль обновлён</span>
                                    <span className="chat-info-value">
                                        {profile.profileUpdatedAt ? new Date(profile.profileUpdatedAt).toLocaleDateString('ru-RU') : '—'}
                                    </span>
                                </div>
                            </div>
                        </div>

                        <div className="chat-info-actions">
                            <button className="chat-info-action-btn primary" onClick={handleSendMessage} disabled={isSending}>
                                <i className="bi bi-chat-dots"></i>
                                {isSending ? 'Создание чата...' : 'Написать сообщение'}
                            </button>
                        </div>
                    </React.Fragment>
                ) : null}
            </div>
        </div>
    );
};