# Admin/admin_client.rb
require 'socket'
require 'google/protobuf'
require_relative 'capacity_pb'

# Plotter bilgileri
PLOTTER_HOST = 'localhost'
PLOTTER_PORT = 8000

# Sunucu bilgileri
SERVERS = [
  { host: 'localhost', port: 5001 },  # Server1
  { host: 'localhost', port: 5002 },  # Server2
  { host: 'localhost', port: 5003 }   # Server3
]

# Plotter'a veri gönder
def send_to_plotter(data)
  begin
    plotter_socket = TCPSocket.new(PLOTTER_HOST, PLOTTER_PORT)
    plotter_socket.write(data)
    plotter_socket.close
  rescue Errno::ECONNREFUSED
    puts "Plotter'a bağlanılamadı: #{PLOTTER_HOST}:#{PLOTTER_PORT}"
  end
end

# Kapasite sorgusu gönder
def query_capacity(host, port)
  begin
    socket = TCPSocket.new(host, port)
    puts "Sunucuya bağlandı: #{host}:#{port}"

    # Capacity nesnesi oluştur
    capacity = Capacity::Capacity.new(
      server_id: 1,  # Sorgulanacak sunucu ID'si
      server_status: 0,  # Başlangıçta boş
      timestamp: Time.now.to_i
    )

    # Capacity nesnesini sunucuya gönder
    serialized_capacity = capacity.to_proto
    socket.write(serialized_capacity)
    puts "Capacity nesnesi sunucuya gönderildi."

    # Sunucudan yanıt al
    response = socket.read
    if response
      received_capacity = Capacity::Capacity.decode(response)
      puts "Sunucudan gelen yanıt:"
      puts "Sunucu ID: #{received_capacity.server_id}"
      puts "Doluluk Oranı: #{received_capacity.server_status}"
      puts "Zaman Damgası: #{Time.at(received_capacity.timestamp)}"

      # Plotter'a veri gönder
      plotter_data = "#{received_capacity.server_id},#{received_capacity.server_status},#{received_capacity.timestamp}"
      send_to_plotter(plotter_data)
      return received_capacity.server_status
    else
      puts "Sunucudan yanıt alınamadı."
    end

  rescue Errno::ECONNREFUSED
    puts "Sunucuya bağlanılamadı: #{host}:#{port}"
  ensure
    socket.close if socket
    puts "Bağlantı kapatıldı."
  end
  nil
end

# Tüm sunucuların doluluk oranlarını sorgula
def query_all_servers
  server_status = {}
  SERVERS.each do |server|
    status = query_capacity(server[:host], server[:port])
    server_status[server[:port]] = status if status
  end
  server_status
end

# Ana program
if __name__ == "__main__"
  loop do
    server_status = query_all_servers
    # Doluluk oranlarını bir dosyaya yaz (istemci bu dosyayı okuyacak)
    File.open("server_status.txt", "w") do |file|
      server_status.each do |port, status|
        file.puts "#{port},#{status}"
      end
    end
    sleep 5  # 5 saniyede bir sorgu gönder
  end
end
