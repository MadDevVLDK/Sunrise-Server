const ChatInfoModal = ({ isOpen, onClose, chat, user, onTogglePin, onLeaveChat, onDeleteChat, onUpdateChatInfo, onOpenUserProfile }) => {
    const [activeTab, setActiveTab] = React.useState('info');
    const [members, setMembers] = React.useState([]);
    const [isLoadingMembers, setIsLoadingMembers] = React.useState(false);
    const [nextCursor, setNextCursor] = React.useState(null);
    const [hasMoreMembers, setHasMoreMembers] = React.useState(false);
    const [isPinned, setIsPinned] = React.useState(false);
    const [isEditing, setIsEditing] = React.useState(false);
    const [editName, setEditName] = React.useState('');
    const [editDescription, setEditDescription] = React.useState('');
    // ✅ Профиль собеседника для личного чата (подгружается если не пришёл с чатом)
    const [opponentProfile, setOpponentProfile] = React.useState(null);

    // ✅ Строковая проверка типа чата
    const isPersonal = chat ? chat.chatType === 'PERSONAL' : false;
    const isGroup = chat ? chat.chatType !== 'PERSONAL' : false;
    const isAdmin = chat ? (chat.rights && chat.rights.isAdmin === true) ||
            (chat.selfMember && chat.selfMember.isAdmin === true) ||
            chat.selfMemberIsAdmin === true : false;

    // ✅ Получаем имя и username собеседника для личного чата
    const opponentData = React.useMemo(() => {
        if (!isPersonal || !chat) return null;
        // Пробуем взять из объекта чата (если сервер заполнил)
        if (chat.opponent && chat.opponent.userProfile) {
            return chat.opponent.userProfile;
        }
        // Если нет — используем загруженный профиль
        if (opponentProfile) return opponentProfile;
        // Заглушка из имени чата
        return { name: chat.name || 'Пользователь', username: '' };
    }, [isPersonal, chat, opponentProfile]);

    React.useEffect(() => {
        if (isOpen && chat) {
            setActiveTab('info');
            setIsEditing(false);
            setIsPinned(Boolean(chat.isPinned));
            setMembers([]);
            setNextCursor(null);
            setHasMoreMembers(false);
            setOpponentProfile(null);

            if (isGroup) {
                loadMembers(null);
            }

            // ✅ Для личного чата подгружаем профиль собеседника если его нет в данных чата
            if (isPersonal && (!chat.opponent || !chat.opponent.userProfile)) {
                loadOpponentProfile();
            }
        }
    }, [isOpen, chat]);

    // ✅ Загрузка профиля собеседника для личного чата
    const loadOpponentProfile = async () => {
        if (!chat) return;
        // opponentId может быть в chat.opponentId или определяться из участников
        const opponentId = chat.opponentId || (chat.opponent && chat.opponent.memberProfile ? chat.opponent.memberProfile.userId : null);
        if (!opponentId) return;
        try {
            const profile = await API.getOtherProfile(opponentId.toString(), true);
            setOpponentProfile(profile);
        } catch (e) {
            console.warn('Не удалось загрузить профиль собеседника', e);
        }
    };

    const loadMembers = async (cursor) => {
        if (!chat || !isGroup) return;
        setIsLoadingMembers(true);
        try {
            const result = await API.getChatMembersPage(chat.id.toString(), cursor, 20);
            const newMembers = result.chatMembers || [];
            setMembers(function(prev) {
                if (!cursor) return newMembers;
                var existingIds = new Set(prev.map(function(m) { return String(m.userId || (m.memberProfile && m.memberProfile.userId)); }));
                var unique = newMembers.filter(function(m) {
                    var uid = String(m.userId || (m.memberProfile && m.memberProfile.userId));
                    return !existingIds.has(uid);
                });
                return prev.concat(unique);
            });
            setHasMoreMembers(Boolean(result.nextCursor));
            setNextCursor(result.nextCursor);
        } catch (e) {
            if (typeof ApiError !== 'undefined' && e instanceof ApiError) {
                Toast.error(e.displayMessage);
            } else {
                Toast.error('Ошибка загрузки участников');
            }
        } finally {
            setIsLoadingMembers(false);
        }
    };

    const handleTogglePin = async () => {
        try {
            await onTogglePin(chat.id, isPinned);
            setIsPinned(!isPinned);
        } catch (e) { /* уже обработано */ }
    };

    const handleSaveEdit = async () => {
        if (!editName.trim()) {
            Toast.warning('Название не может быть пустым');
            return;
        }
        var toastId = Toast.loading('Сохранение...');
        try {
            await onUpdateChatInfo(chat.id, editName.trim(), editDescription.trim());
            Toast.dismiss(toastId);
            Toast.success('Информация обновлена');
            setIsEditing(false);
        } catch (e) {
            Toast.dismiss(toastId);
            if (typeof ApiError !== 'undefined' && e instanceof ApiError) {
                Toast.error(e.displayMessage);
            } else {
                Toast.error('Ошибка обновления');
            }
        }
    };

    const startEdit = () => {
        setEditName(chat ? (chat.name || '') : '');
        setEditDescription(chat ? (chat.description || '') : '');
        setIsEditing(true);
    };

    if (!isOpen || !chat) return null;

    const getInitials = (name) => {
        if (!name) return 'Ч';
        return name.split(' ').map(function(w) { return w[0]; }).join('').toUpperCase().slice(0, 2);
    };

    // ✅ Определяем отображаемое имя
    const displayName = isPersonal
        ? (opponentData ? opponentData.name : chat.name || 'Пользователь')
        : (chat.name || 'Группа');

    return (
        <div className="modal-overlay" onClick={onClose}>
            <div className="chat-info-modal" onClick={function(e) { e.stopPropagation(); }}>
                <div className="chat-info-header">
                    <h3>{isPersonal ? 'О пользователе' : 'О группе'}</h3>
                    <button className="chat-info-close" onClick={onClose}>
                        <i className="bi bi-x-lg"></i>
                    </button>
                </div>

                <div className="chat-info-profile">
                    <div className="chat-info-avatar">{getInitials(displayName)}</div>

                    {!isEditing ? (
                        <React.Fragment>
                            <div className="chat-info-name">{displayName}</div>
                            <div className="chat-info-subtitle">
                                {isPersonal
                                    ? (opponentData && opponentData.username ? '@' + opponentData.username : 'В сети')
                                    : (chat.membersCount || 0) + ' участников'}
                            </div>
                            {isGroup && chat.description ? (
                                <div className="chat-info-description">{chat.description}</div>
                            ) : null}
                        </React.Fragment>
                    ) : (
                        <div className="chat-info-edit-fields">
                            <label className="chat-info-edit-label">
                                <i className="bi bi-chat-text"></i>
                                Название {isGroup ? 'группы' : 'чата'} <span style={{ color: 'var(--error-color)' }}>*</span>
                            </label>
                            <input type="text" className="create-chat-input" placeholder="Введите название"
                                    value={editName} onChange={function(e) { setEditName(e.target.value); }} maxLength={30} />

                            <label className="chat-info-edit-label">
                                <i className="bi bi-card-text"></i>
                                Описание
                            </label>
                            <input type="text" className="create-chat-input" placeholder="Краткое описание (необязательно)"
                                    value={editDescription} onChange={function(e) { setEditDescription(e.target.value); }} maxLength={500} />

                            <div className="chat-info-edit-actions">
                                <button className="chat-info-btn-secondary" onClick={function() { setIsEditing(false); }}>Отмена</button>
                                <button className="chat-info-btn-primary" onClick={handleSaveEdit}>Сохранить</button>
                            </div>
                        </div>
                    )}
                </div>

                {isGroup ? (
                    <div className="chat-info-tabs">
                        <button className={'chat-info-tab' + (activeTab === 'info' ? ' active' : '')}
                            onClick={function() { setActiveTab('info'); }}>
                            <i className="bi bi-info-circle"></i> Информация
                        </button>
                        <button className={'chat-info-tab' + (activeTab === 'members' ? ' active' : '')}
                            onClick={function() { setActiveTab('members'); }}>
                            <i className="bi bi-people"></i> Участники ({chat.membersCount || 0})
                        </button>
                    </div>
                ) : null}

                <div className="chat-info-content">
                    {(!isGroup || activeTab === 'info') ? (
                        <React.Fragment>
                            <div className="chat-info-details">
                                <div className="chat-info-row">
                                    <i className="bi bi-chat-dots"></i>
                                    <span>Тип чата</span>
                                    <span className="chat-info-value">{isPersonal ? 'Личный' : 'Групповой'}</span>
                                </div>
                                <div className="chat-info-row">
                                    <i className="bi bi-calendar"></i>
                                    <span>Создан</span>
                                    <span className="chat-info-value">
                                        {chat.createdAt ? new Date(chat.createdAt).toLocaleDateString('ru-RU') : '—'}
                                    </span>
                                </div>
                                {isGroup ? (
                                    <div className="chat-info-row">
                                        <i className="bi bi-person-check"></i>
                                        <span>Ваша роль</span>
                                        <span className="chat-info-value">{isAdmin ? 'Администратор' : 'Участник'}</span>
                                    </div>
                                ) : null}
                            </div>
                        </React.Fragment>
                    ) : null}

                    {isGroup && activeTab === 'members' ? (
                        <div className="chat-info-members">
                            {isLoadingMembers && members.length === 0 ? (
                                <div className="chat-info-loading">
                                    <div className="sunrise-toast-spinner"></div>
                                    <span>Загрузка участников...</span>
                                </div>
                            ) : null}
                            {members.map(function(member, idx) {
                                var mProfile = member.memberProfile || member;
                                var uProfile = member.userProfile || {};
                                var uid = String(mProfile.userId || member.userId || idx);
                                var mName = uProfile.name || uProfile.username || member.name || 'Пользователь';
                                var mUsername = uProfile.username || member.username || '';
                                var mIsAdmin = mProfile.isAdmin === true || member.isAdmin === true;
                                return (
                                    <div key={uid} className="chat-info-member-item chat-info-member-clickable"
                                        onClick={function() { if (onOpenUserProfile) onOpenUserProfile(uid); }}>
                                        <div className="chat-info-member-avatar">{getInitials(mName)}</div>
                                        <div className="chat-info-member-info">
                                            <div className="chat-info-member-name">{mName}</div>
                                            <div className="chat-info-member-username">@{mUsername || 'unknown'}</div>
                                        </div>
                                        {mIsAdmin ? (
                                            <span className="chat-info-member-badge">Админ</span>
                                        ) : null}
                                    </div>
                                );
                            })}
                            {hasMoreMembers && !isLoadingMembers ? (
                                <button className="chat-info-load-more" onClick={function() { loadMembers(nextCursor); }}>
                                    Загрузить ещё
                                </button>
                            ) : null}
                            {!isLoadingMembers && members.length === 0 ? (
                                <div className="chat-info-empty">Нет участников</div>
                            ) : null}
                        </div>
                    ) : null}
                </div>

                <div className="chat-info-actions">
                    <button className="chat-info-action-btn" onClick={handleTogglePin}>
                        <i className={'bi ' + (isPinned ? 'bi-pin-angle-fill' : 'bi-pin-angle')}></i>
                        {isPinned ? 'Открепить чат' : 'Закрепить чат'}
                    </button>
                    {isGroup && isAdmin && !isEditing ? (
                        <button className="chat-info-action-btn" onClick={startEdit}>
                            <i className="bi bi-pencil"></i>
                            Изменить информацию
                        </button>
                    ) : null}
                    <button className="chat-info-action-btn danger" onClick={function() { onLeaveChat(chat.id); onClose(); }}>
                        <i className="bi bi-box-arrow-right"></i>
                        {isGroup ? 'Покинуть группу' : 'Удалить чат'}
                    </button>
                    {isGroup && isAdmin ? (
                        <button className="chat-info-action-btn danger" onClick={function() { onDeleteChat(chat.id); onClose(); }}>
                            <i className="bi bi-trash"></i>
                            Удалить группу для всех
                        </button>
                    ) : null}
                </div>
            </div>
        </div>
    );
};