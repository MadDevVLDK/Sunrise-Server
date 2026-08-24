const CreateChatModal = ({ isOpen, onClose, onChatCreated }) => {
    const [activeTab, setActiveTab] = React.useState('personal');
    const [searchQuery, setSearchQuery] = React.useState('');
    const [searchResults, setSearchResults] = React.useState([]);
    const [isSearching, setIsSearching] = React.useState(false);
    const [selectedUsers, setSelectedUsers] = React.useState([]);
    const [groupName, setGroupName] = React.useState('');
    const [groupDescription, setGroupDescription] = React.useState('');
    const searchTimeoutRef = React.useRef(null);

    // Поиск пользователей с дебаунсом
    const handleSearch = React.useCallback((query) => {
        setSearchQuery(query);
        if (searchTimeoutRef.current) clearTimeout(searchTimeoutRef.current);
        if (!query || query.trim().length < 2) {
            setSearchResults([]);
            return;
        }
        searchTimeoutRef.current = setTimeout(async () => {
            setIsSearching(true);
            try {
                const result = await API.getActiveUsersPage(query.trim(), null, 20);
                setSearchResults(result.users || []);
            } catch (e) {
                Toast.error('Ошибка поиска пользователей');
            } finally {
                setIsSearching(false);
            }
        }, 400);
    }, []);

    const toggleUser = (user) => {
        setSelectedUsers(prev => {
            const exists = prev.some(u => u.id === user.id);
            if (exists) return prev.filter(u => u.id !== user.id);
            if (prev.length >= 100) {
                Toast.warning('Максимум 100 участников');
                return prev;
            }
            return [...prev, user];
        });
    };

    const removeUser = (userId) => {
        setSelectedUsers(prev => prev.filter(u => u.id !== userId));
    };

    const handleCreate = async () => {
        const toastId = Toast.loading('Создание чата...');
        try {
            if (activeTab === 'personal') {
                if (selectedUsers.length !== 1) {
                    Toast.dismiss(toastId);
                    Toast.warning('Выберите одного пользователя для личного чата');
                    return;
                }
                const tempId = `temp_${Date.now()}`;
                const chatId = await API.createPersonalChat(tempId, selectedUsers[0].id);
                Toast.dismiss(toastId);
                Toast.success('Личный чат создан');
                resetForm();
                onClose();
                if (onChatCreated) onChatCreated(chatId);
            } else {
                if (!groupName.trim()) {
                    Toast.dismiss(toastId);
                    Toast.warning('Укажите название группы');
                    return;
                }
                if (selectedUsers.length === 0) {
                    Toast.dismiss(toastId);
                    Toast.warning('Добавьте хотя бы одного участника');
                    return;
                }
                const tempId = `temp_${Date.now()}`;
                const memberIds = selectedUsers.map(u => u.id);
                const chatId = await API.createGroupChat(tempId, groupName.trim(), groupDescription.trim(), memberIds);
                Toast.dismiss(toastId);
                Toast.success('Групповой чат создан');
                resetForm();
                onClose();
                if (onChatCreated) onChatCreated(chatId);
            }
        } catch (e) {
            Toast.dismiss(toastId);
            if (e instanceof ApiError) {
                Toast.error(e.displayMessage);
            } else {
                Toast.error('Ошибка создания чата');
            }
        }
    };

    const resetForm = () => {
        setSearchQuery('');
        setSearchResults([]);
        setSelectedUsers([]);
        setGroupName('');
        setGroupDescription('');
        setActiveTab('personal');
    };

    const handleClose = () => {
        resetForm();
        onClose();
    };

    if (!isOpen) return null;

    return (
        <div className="modal-overlay" onClick={handleClose}>
            <div className="create-chat-modal" onClick={(e) => e.stopPropagation()}>
                {/* Заголовок */}
                <div className="create-chat-header">
                    <h3>Создать чат</h3>
                    <button className="create-chat-close" onClick={handleClose}>
                        <i className="bi bi-x-lg"></i>
                    </button>
                </div>

                {/* Табы */}
                <div className="create-chat-tabs">
                    <button
                        className={`create-chat-tab ${activeTab === 'personal' ? 'active' : ''}`}
                        onClick={() => { setActiveTab('personal'); setSelectedUsers([]); }}
                    >
                        <i className="bi bi-person"></i> Личный
                    </button>
                    <button
                        className={`create-chat-tab ${activeTab === 'group' ? 'active' : ''}`}
                        onClick={() => setActiveTab('group')}
                    >
                        <i className="bi bi-people"></i> Групповой
                    </button>
                </div>

                {/* Поля для группового чата */}
                {activeTab === 'group' && (
                    <div className="create-chat-group-fields">
                        <input
                            type="text"
                            className="create-chat-input"
                            placeholder="Название группы *"
                            value={groupName}
                            onChange={(e) => setGroupName(e.target.value)}
                            maxLength={30}
                        />
                        <input
                            type="text"
                            className="create-chat-input"
                            placeholder="Описание (необязательно)"
                            value={groupDescription}
                            onChange={(e) => setGroupDescription(e.target.value)}
                            maxLength={500}
                        />
                    </div>
                )}

                {/* Поиск */}
                <div className="create-chat-search">
                    <i className="bi bi-search"></i>
                    <input
                        type="text"
                        className="create-chat-search-input"
                        placeholder={activeTab === 'personal' ? 'Поиск пользователя...' : 'Добавить участников...'}
                        value={searchQuery}
                        onChange={(e) => handleSearch(e.target.value)}
                    />
                    {isSearching && <div className="sunrise-toast-spinner" style={{ width: 16, height: 16, borderWidth: 2 }}></div>}
                </div>

                {/* Выбранные пользователи */}
                {selectedUsers.length > 0 && (
                    <div className="create-chat-selected">
                        {selectedUsers.map(u => (
                            <span key={u.id} className="create-chat-selected-user">
                                {u.name}
                                <button onClick={() => removeUser(u.id)}>&times;</button>
                            </span>
                        ))}
                    </div>
                )}

                {/* Результаты поиска */}
                <div className="create-chat-results">
                    {searchResults.length === 0 && searchQuery.trim().length >= 2 && !isSearching && (
                        <div className="create-chat-empty">Пользователи не найдены</div>
                    )}
                    {searchResults.map(user => {
                        const isSelected = selectedUsers.some(u => u.id === user.id);
                        return (
                            <div
                                key={user.id}
                                className={`create-chat-user-item ${isSelected ? 'selected' : ''}`}
                                onClick={() => {
                                    if (activeTab === 'personal') {
                                        setSelectedUsers([user]);
                                    } else {
                                        toggleUser(user);
                                    }
                                }}
                            >
                                <div className="create-chat-user-avatar">
                                    {(user.name || '').split(' ').map(w => w[0]).join('').toUpperCase().slice(0, 2)}
                                </div>
                                <div className="create-chat-user-info">
                                    <div className="create-chat-user-name">{user.name}</div>
                                    <div className="create-chat-user-username">@{user.username}</div>
                                </div>
                                {isSelected && <i className="bi bi-check-circle-fill create-chat-check"></i>}
                            </div>
                        );
                    })}
                </div>

                {/* Кнопка создания */}
                <div className="create-chat-footer">
                    <button
                        className="btn-create-chat"
                        onClick={handleCreate}
                        disabled={
                            activeTab === 'personal' ? selectedUsers.length !== 1 :
                            (!groupName.trim() || selectedUsers.length === 0)
                        }
                    >
                        <i className="bi bi-plus-circle"></i>
                        Создать чат
                    </button>
                </div>
            </div>
        </div>
    );
};