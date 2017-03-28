require "net/http"
require "json"
require "base64"

notification = {
  :message => "Teodor Pripoae commented on your post",
  :avatar => "https://avatars3.githubusercontent.com/u/393437?v=3&s=460",
  :notifiable_ref_id => "9ebb963e-69fc-4ce3-88ce-4b8863e7a971",
  :kind => "post_comment"
}

data = {
  :subscription => "backendapp_notifications_publish",
  :message => {
    :attributes => {},
    :data => Base64.strict_encode64(notification.to_json)
  }
}

uri = URI("http://localhost:8888/_pubsub/task/main.notifications.publish")
req = Net::HTTP::Post.new(uri, 'Content-Type' => 'application/json')
req.body = data.to_json
res = Net::HTTP.start(uri.hostname, uri.port) do |http|
  http.request(req)
end

puts "Status: #{res.code} body: #{res.body}"
