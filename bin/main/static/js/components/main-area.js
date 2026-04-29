/**
 * Компонент основной области (справа)
 */
const MainArea = ({ selectedChatId, onChatUpdated, allChatIds }) => {
    if (!selectedChatId) {
        return (
            <div className="main-area">
                <div className="placeholder-icon"><i className="bi bi-chat-dots"></i></div>
                <div className="placeholder-text">Нажмите на чат, чтобы начать общение!</div>
                <div className="placeholder-subtext">Выберите чат из списка слева</div>
            </div>
        );
    }
    return (
        <div className="main-area">
            <div className="placeholder-icon"><i className="bi bi-hourglass-split"></i></div>
            <div className="placeholder-text">Чат {selectedChatId} будет доступен позже</div>
            <div className="placeholder-subtext">Разработка окна сообщений</div>
        </div>
    );
};