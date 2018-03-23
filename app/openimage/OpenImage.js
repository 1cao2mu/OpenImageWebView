import React, { Component } from 'react';
import {
  AppRegistry, StyleSheet, Text, View, Platform, Button, Alert, ActivityIndicator, Linking, WebView
} from 'react-native';
import WebView2 from './WebView2';

export default class OpenImage extends Component {

  render() {
    return (
      <WebView2
        ref='webs'
        scalesPageToFit={true}
        bounces={false}
        source={{ uri: "http://172.18.239.16:3000/" }}
        style={{ flex: 1 }}
        mixedContentMode="always"
        startInLoadingState={true}
        domStorageEnabled={false}
        javaScriptEnabled={true}
      />
    );
  }
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: '#F5FCFF',
  }
});