/**
 * Компонент меню пользователя
 */
const UserMenu = ({ user, isOpen, onToggle, onLogout, onProfile }) => {
    if (!user) return null;

    // Получить инициалы для аватара
    const getInitials = (name) => {
        return name.split(' ').map(word => word[0]).join('').toUpperCase().slice(0, 2);
    };

    return (
        <>
            <button className="user-menu-trigger" onClick={onToggle}>
                <div className="user-avatar-small">
                    {getInitials(user.name)}
                </div>
                <div className="user-info-small">
                    <div className="user-name-small">{user.name}</div>
                    <div className="user-status-small">@{user.username}</div>
                </div>
                <i className="bi bi-chevron-up"></i>
            </button>

            {isOpen && (
                <div className="user-menu-dropdown">
                    <button className="menu-item" onClick={onProfile}>
                        <i className="bi bi-person"></i>
                        <span>Профиль</span>
                    </button>
                    <button className="menu-item">
                        <i className="bi bi-gear"></i>
                        <span>Настройки</span>
                    </button>
                    <hr style={{ margin: '8px 0', border: 'none', borderTop: '1px solid var(--border-color)' }} />
                    <button className="menu-item danger" onClick={onLogout}>
                        <i className="bi bi-box-arrow-right"></i>
                        <span>Выход</span>
                    </button>
                </div>
            )}
        </>
    );
};

