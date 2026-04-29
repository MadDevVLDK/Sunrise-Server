/**
 * Компонент боковой панели (меню + список чатов)
 */
const Sidebar = ({ user, chats, selectedChatId, onSelectChat, isLoadingChats, isMenuOpen, onToggleMenu, onLogout, onProfile, onLoadMore, hasMoreChats, onTogglePin }) => {
    const [searchQuery, setSearchQuery] = React.useState('');

    // Фильтровать чаты по поисковому запросу
    const filteredChats = React.useMemo(() => {
        console.log('[Sidebar] filteredChats recomputed, input chats length:', chats?.length);
        if (!Array.isArray(chats) || !searchQuery.trim()) return chats;

        const query = searchQuery.toLowerCase();
        return chats.filter(chat => chat.name.toLowerCase().includes(query));
    }, [chats, searchQuery]);

    return (
        <div className="messenger-sidebar">
            {/* Header с логотипом и кнопками */}
            <div className="sidebar-header">
                <div className="sidebar-brand">
                    <i className="bi bi-sun-fill"/>
                    <span>Sunrise</span>
                </div>
                <div className="sidebar-actions">
                    <button className="sidebar-btn" title="Создать чат">
                        <i className="bi bi-pencil-square"/>
                    </button>
                    <button className="sidebar-btn" title="Параметры">
                        <i className="bi bi-sliders"/>
                    </button>
                </div>
            </div>

            {/* Поиск */}
            <div className="sidebar-search">
                <input
                    type="text"
                    className="search-input"
                    placeholder="Поиск чатов..."
                    value={searchQuery}
                    onChange={(e) => setSearchQuery(e.target.value)}
                />
            </div>

            {/* Список чатов */}
            <ChatList
                chats={filteredChats}
                selectedChatId={selectedChatId}
                onSelectChat={onSelectChat}
                isLoading={isLoadingChats}
                onLoadMore={onLoadMore}
                hasMoreChats={hasMoreChats}
                onTogglePin={onTogglePin}
            />

            {/* Меню пользователя */}
            <div className="sidebar-footer" style={{ position: 'relative' }}>
                <UserMenu
                    user={user}
                    isOpen={isMenuOpen}
                    onToggle={onToggleMenu}
                    onLogout={onLogout}
                    onProfile={onProfile}
                />
            </div>
        </div>
    );
};

