package com.bcilab.lonelyelderly;

// Java program to implement a array using an array
class Queue {
    private int front, rear, capacity;
    private float threshold;
    private float array[];

    public int getNumOverTH(){
        int num = 0;

        if(isEmpty())
            return num;

        for(int i = 0; i < capacity; i++) {
            if(array[i] > threshold)
                num++;
        }

        return num;
    }

    public int getNumOverST(float st){
        int num = 0;

        if(isEmpty())
            return num;

        for(int i = 0; i < capacity; i++) {
            if(array[i] > st)
                num++;
        }

        return num;
    }

    Queue(int c, float th) {
        front = rear = 0;
        capacity = c;
        threshold = th;
        array = new float[capacity];
    }

    // function to insert an element
    // at the rear of the array
    synchronized void Enqueue(float data)
    {
        array[rear] = data;
        rear++;
        return;
    }

    // function to delete an element
    // from the front of the array
    synchronized void Dequeue()
    {
        // if array is empty
        if (isEmpty())
            return;

            // shift all the elements from index 2 till rear
            // to the right by one
        else {
            for (int i = 0; i < rear - 1; i++) {
                array[i] = array[i + 1];
            }

            // store 0 at rear indicating there's no element
            if (rear < capacity)
                array[rear] = 0;

            // decrement rear
            rear--;
        }
        return;
    }

    public boolean isFull(){
        // check array is not full
        if(capacity == rear)
            return true;
        else
            return false;
    }

    public boolean isEmpty(){
        // if array is empty
        if (front == rear)
            return true;
        else
            return false;
    }
}