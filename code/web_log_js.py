import time
import json
import numpy as np
from flask import Flask, render_template
from flask_socketio import SocketIO
from kafka import KafkaConsumer

app = Flask(__name__)
app.config['SECRET_KEY'] = 'secret!'
socketio = SocketIO(app)
thread = None
# 实例化一个consumer，接收topic为result的消息
consumer = KafkaConsumer('restule',bootstrap_servers="192.168.1.185:9092")


# 一个后台线程，持续接收Kafka消息，并发送给客户端浏览器
def background_thread():

    for msg in consumer:
        data_json = msg.value.decode('utf-8')
        data_list = json.loads(data_json)
        baidu = 0
        google = 0
        sogou = 0
        yahoo = 0
        bing = 0
        for data in data_list:
            if 'www.baidu.com' in data.keys():
                baidu = data['www.baidu.com']
            elif 'www.google.cn' in data.keys():
                google = data['www.google.cn']
            elif 'www.sogou.com' in data.keys():
                sogou = data['www.sogou.com']
            elif 'www.yahoo.com' in data.keys():
                yahoo = data['www.yahoo.com']
            elif 'cn.bing.com' in data.keys():
                bing = data['cn.bing.com']
            else:
                continue




        print(baidu,google,yahoo,sogou,bing)
        socketio.emit('test_message', {'baidu': baidu,'yahoo':yahoo,'google':google,'sogou':sogou,'bing':bing})
        # count += 1
        # time.sleep(1)


# 客户端发送connect事件时的处理函数
@socketio.on('test_connects')
def connect(message):
    print(message)
    global thread
    if thread is None:
        # 单独开启一个线程给客户端发送数据
        thread = socketio.start_background_task(target=background_thread)
    socketio.emit('connected', {'data': 'Connected'})


# 通过访问http://127.0.0.1:5000/访问index.html
@app.route("/")
def handle_mes():
    return render_template("web_log.html")


# main函数
if __name__ == '__main__':
    socketio.run(app, debug=True)
