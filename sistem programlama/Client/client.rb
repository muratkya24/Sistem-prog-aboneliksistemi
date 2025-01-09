# Client/client.rb
require 'socket'
require 'google/protobuf'
require_relative 'subscriber_pb'

# Sunucu bilgileri
SERVERS = [
  { host: 'localhost', port: 5001 },  # Server1
  { host: 'localhost', port: 5002 },  # Server2
  { host: 'localhost', port: 5003 }   # Server3
]

# Abonelik işlemleri
def subscribe(host, port, name_surname, interests)
  begin
    socket = TCPSocket.new(host, port)
    puts "Sunucuya bağlandı: #{host}:#{port}"

    # Subscriber nesnesi oluştur
    subscriber = Subscriber::Subscriber.new(
      id: 0,  # ID sunucu tarafından atanacak
      name_surname: name_surname,
      start_date: Time.now.to_i,
      last_accessed: Time.now.to_i,
      interests: interests,
      isOnline: false,
      demand: Subscriber::Subscriber::DemandType::SUBS
    )

    # Subscriber nesnesini sunucuya gönder
    serialized_subscriber = subscriber.to_proto
    socket.write(serialized_subscriber)
    puts "Abonelik isteği gönderildi: #{name_surname}"

    # Sunucudan yanıt al
    response = socket.read
    if response
      received_subscriber = Subscriber::Subscriber.decode(response)
      puts "Abonelik başarılı! Abone ID: #{received_subscriber.id}"
      return received_subscriber.id
    else
      puts "Sunucudan yanıt alınamadı."
    end

  rescue Errno::ECONNREFUSED, Errno::ETIMEDOUT => e
    puts "Sunucuya bağlanılamadı: #{host}:#{port} - #{e.message}"
    return nil
  ensure
    socket.close if socket
    puts "Bağlantı kapatıldı."
  end
end

# En az dolu sunucuyu bul
def find_least_loaded_server
  server_status = {}
  File.readlines("server_status.txt").each do |line|
    port, status = line.chomp.split(',')
    server_status[port.to_i] = status.to_i
  end

  # En az dolu sunucuyu seç
  least_loaded_port = server_status.min_by { |_, status| status }&.first
  SERVERS.find { |server| server[:port] == least_loaded_port }
end

# Ana program
if __name__ == "__main__"
  # En az dolu sunucuyu bul
  server = find_least_loaded_server
  if server
    # Yeni abonelik oluştur
    subscriber_id = subscribe(server[:host], server[:port], "Jane Doe", ["sports", "technology"])

    if subscriber_id
      # Abone giriş yap
      login(server[:host], server[:port], subscriber_id)

      # Abone çıkış yap
      logout(server[:host], server[:port], subscriber_id)
    end
  else
    puts "Tüm sunucular dolu veya bağlanılamıyor."
  end
end
