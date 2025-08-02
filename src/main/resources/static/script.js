document.addEventListener('DOMContentLoaded', () => {
    const drawButton = document.getElementById('drawButton');
    const resultMessage = document.getElementById('resultMessage');
    const prizeDisplay = document.getElementById('prizeDisplay');

    drawButton.addEventListener('click', async () => {
        resultMessage.textContent = '抽奖中...';
        prizeDisplay.textContent = '';
        resultMessage.style.color = '#333'; // 重置颜色

        try {
            const response = await fetch('/lottery/draw', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                }
            });

            const data = await response.json(); // 解析 JSON 响应

            if (data.code === 200) {
                resultMessage.textContent = data.message;
                resultMessage.style.color = '#4CAF50'; // 绿色表示中奖
                prizeDisplay.textContent = `恭喜您获得：${data.data}`;
            } else {
                resultMessage.textContent = data.message;
                resultMessage.style.color = '#d9534f'; // 红色表示未中奖
                prizeDisplay.textContent = '';
            }
        } catch (error) {
            console.error('抽奖请求失败:', error);
            resultMessage.textContent = '抽奖失败，请稍后再试。';
            resultMessage.style.color = '#d9534f';
            prizeDisplay.textContent = '';
        }
    });
});
