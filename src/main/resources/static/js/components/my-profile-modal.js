const MyProfileModal = ({ isOpen, onClose, user, onProfileUpdated }) => {
    const [profile, setProfile] = React.useState(null);
    const [isLoading, setIsLoading] = React.useState(false);
    const [isEditing, setIsEditing] = React.useState(false);
    const [isSaving, setIsSaving] = React.useState(false);
    const [editName, setEditName] = React.useState('');
    const [editUsername, setEditUsername] = React.useState('');

    React.useEffect(() => {
        if (isOpen) {
            setIsEditing(false);
            loadProfile();
        }
    }, [isOpen]);

    const loadProfile = async () => {
        setIsLoading(true);
        try {
            const result = await API.getMyProfile();
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

    const startEdit = () => {
        setEditName(profile ? profile.name || '' : '');
        setEditUsername(profile ? profile.username || '' : '');
        setIsEditing(true);
    };

    const handleSave = async () => {
        if (!editName.trim() || editName.trim().length < 2) {
            Toast.warning('Имя: минимум 2 символа');
            return;
        }
        if (!editUsername.trim() || editUsername.trim().length < 4) {
            Toast.warning('Имя пользователя: минимум 4 символа');
            return;
        }
        setIsSaving(true);
        try {
            await API.updateProfile(editUsername.trim(), editName.trim());
            Toast.success('Профиль обновлён');
            setIsEditing(false);
            await loadProfile();
            if (onProfileUpdated) onProfileUpdated(editUsername.trim(), editName.trim());
        } catch (e) {
            if (typeof ApiError !== 'undefined' && e instanceof ApiError) {
                Toast.error(e.displayMessage);
            } else {
                Toast.error('Ошибка обновления профиля');
            }
        } finally {
            setIsSaving(false);
        }
    };

    const handleDeleteAccount = async () => {
        if (!confirm('Вы уверены, что хотите удалить аккаунт? Это действие необратимо.')) return;
        var toastId = Toast.loading('Удаление аккаунта...');
        try {
            await API.deleteProfile();
            Toast.dismiss(toastId);
            Toast.success('Аккаунт удалён');
            AuthService.logout();
        } catch (e) {
            Toast.dismiss(toastId);
            if (typeof ApiError !== 'undefined' && e instanceof ApiError) {
                Toast.error(e.displayMessage);
            } else {
                Toast.error('Ошибка удаления аккаунта');
            }
        }
    };

    if (!isOpen) return null;

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
                    <h3>Мой профиль</h3>
                    <button className="chat-info-close" onClick={onClose}>
                        <i className="bi bi-x-lg"></i>
                    </button>
                </div>

                {isLoading ? (
                    <div className="chat-info-loading" style={{ padding: '40px 24px' }}>
                        <div className="sunrise-toast-spinner"></div>
                        <span>Загрузка профиля...</span>
                    </div>
                ) : (
                    <React.Fragment>
                        <div className="chat-info-profile">
                            <div className="chat-info-avatar">{getInitials(displayName)}</div>

                            {!isEditing ? (
                                <React.Fragment>
                                    <div className="chat-info-name">{displayName}</div>
                                    <div className="chat-info-subtitle">
                                        {displayUsername ? '@' + displayUsername : ''}
                                    </div>
                                </React.Fragment>
                            ) : (
                                <div className="chat-info-edit-fields">
                                    <label className="chat-info-edit-label">
                                        <i className="bi bi-person-badge"></i>
                                        Ваше имя <span style={{ color: 'var(--error-color)' }}>*</span>
                                    </label>
                                    <input type="text" className="create-chat-input" placeholder="Как вас зовут"
                                            value={editName} onChange={function(e) { setEditName(e.target.value); }} maxLength={100} />

                                    <label className="chat-info-edit-label">
                                        <i className="bi bi-at"></i>
                                        Имя пользователя <span style={{ color: 'var(--error-color)' }}>*</span>
                                    </label>
                                    <input type="text" className="create-chat-input" placeholder="Уникальный @username"
                                            value={editUsername} onChange={function(e) { setEditUsername(e.target.value); }} maxLength={50} />

                                    <div className="chat-info-edit-actions">
                                        <button className="chat-info-btn-secondary" onClick={function() { setIsEditing(false); }}>
                                        Отмена
                                        </button>
                                        <button className="chat-info-btn-primary" onClick={handleSave} disabled={isSaving}>
                                        {isSaving ? 'Сохранение...' : 'Сохранить'}
                                        </button>
                                    </div>
                                </div>
                            )}
                        </div>

                        {!isEditing && profile ? (
                            <div className="chat-info-content">
                                <div className="chat-info-details">
                                    <div className="chat-info-row">
                                        <i className="bi bi-at"></i>
                                        <span>Имя пользователя</span>
                                        <span className="chat-info-value">@{displayUsername || '—'}</span>
                                    </div>
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
                        ) : null}

                        <div className="chat-info-actions">
                            {!isEditing ? (
                                <button className="chat-info-action-btn" onClick={startEdit}>
                                    <i className="bi bi-pencil"></i>
                                    Редактировать профиль
                                </button>
                            ) : null}
                            <button className="chat-info-action-btn danger" onClick={handleDeleteAccount}>
                                <i className="bi bi-trash"></i>
                                Удалить аккаунт
                            </button>
                        </div>
                    </React.Fragment>
                )}
            </div>
        </div>
    );
};