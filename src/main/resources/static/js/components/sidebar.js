/**
 * Компонент боковой панели (меню + список чатов)
 */
const Sidebar = ({ user, chats, selectedChatId, onSelectChat, isLoadingChats, isMenuOpen, onToggleMenu, onLogout, onProfile, onTogglePin }) => {
    const [searchQuery, setSearchQuery] = React.useState('');

    const filteredChats = React.useMemo(() => {
        if (!Array.isArray(chats) || !searchQuery.trim()) return chats;
        const query = searchQuery.toLowerCase();
        return chats.filter(chat => chat.name.toLowerCase().includes(query));
    }, [chats, searchQuery]);

    return (
        <div className="messenger-sidebar">
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

            <div className="sidebar-search">
                <input type="text" className="search-input" placeholder="Поиск чатов..." value={searchQuery} onChange={(e) => setSearchQuery(e.target.value)} />
            </div>

            <ChatList
                chats={filteredChats}
                selectedChatId={selectedChatId}
                onSelectChat={onSelectChat}
                isLoading={isLoadingChats}
                onTogglePin={onTogglePin}
            />

            <div className="sidebar-footer" style={{ position: 'relative' }}>
                <UserMenu user={user} isOpen={isMenuOpen} onToggle={onToggleMenu} onLogout={onLogout} onProfile={onProfile} />
            </div>
        </div>
    );
};