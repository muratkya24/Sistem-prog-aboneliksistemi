# Plotter/plotter.py
import matplotlib.pyplot as plt
import socket
import threading
from datetime import datetime

# Sunucu doluluk oranlarını saklamak için sözlük
server_status = {
    'Server1': {'timestamps': [], 'status': []},
    'Server2': {'timestamps': [], 'status': []},
    'Server3': {'timestamps': [], 'status': []}
}

# Renk paleti (her sunucu için farklı renk)
colors = {
    'Server1': 'blue',
    'Server2': 'green',
    'Server3': 'red'
}

# Soket bilgileri
HOST = 'localhost'
PORT = 8000

# Veri alma fonksiyonu
def receive_data():
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
        s.bind((HOST, PORT))
        s.listen()
        print(f"Plotter dinliyor: {HOST}:{PORT}")
        while True:
            conn, addr = s.accept()
            with conn:
                print(f"{addr} bağlandı.")
                data = conn.recv(1024).decode()
                if data:
                    server_id, status, timestamp = data.split(',')
                    server_name = f'Server{server_id}'
                    server_status[server_name]['timestamps'].append(float(timestamp))
                    server_status[server_name]['status'].append(int(status))
                    print(f"Alınan veri: {server_name}, Doluluk: {status}, Zaman: {timestamp}")

# Grafik çizme fonksiyonu
def plot_capacity():
    plt.clf()  # Önceki grafiği temizle

    for server, data in server_status.items():
        if data['timestamps']:  # Veri varsa çiz
            # Zaman damgalarını tarih/saat formatına dönüştür
            timestamps = [datetime.fromtimestamp(ts) for ts in data['timestamps']]
            plt.plot(timestamps, data['status'], label=server, color=colors[server], marker='o')

    plt.xlabel('Zaman')
    plt.ylabel('Doluluk Oranı')
    plt.title('Sunucu Doluluk Oranları')
    plt.legend()  # Sunucu etiketlerini göster
    plt.grid(True)  # Izgara ekle
    plt.xticks(rotation=45)  # Zaman eksenini döndür
    plt.tight_layout()  # Grafiği sıkıştır
    plt.pause(5)  # 5 saniyede bir güncelle

# Ana program
if __name__ == "__main__":
    # Veri alma işlemi için ayrı bir thread başlat
    threading.Thread(target=receive_data, daemon=True).start()

    # Grafik çizme döngüsü
    while True:
        plot_capacity()