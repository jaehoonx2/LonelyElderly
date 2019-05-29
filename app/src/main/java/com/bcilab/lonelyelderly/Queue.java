package com.bcilab.lonelyelderly;

// Java program to implement a array using an array
class Queue {
    private int front, rear, capacity;
    private float threshold;
    private float array[];

    public int getRear() {
        return rear;
    }

    public int getCapacity() {
        return capacity;
    }

    public synchronized void setEmpty() { front = rear = 0; }

    public int getDiffNum(){
        int num = 0;

        if(isEmpty())
            return num;

        for(int i = 1; i < capacity; i++) {
            if (array[i] - array[i-1] > threshold)
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
//        // check array is full or not
//        if (isFull())
//            return;
//
//        // insert element at the rear
//        else {
            array[rear] = data;
            rear++;
//        }
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

    public float getAbsSum(){
        float sum = 0.0f;

        if (isEmpty()) {            // if array is empty
            return 0;
        } else if(!isFull()) {   // check array is not full
            return -1;
        }

        for(int i = 0; i < capacity; i++)
            sum += Math.abs(array[i]);

        return sum;
    }

    public float getAbsAverage(){
        float avg = getAbsSum();

        if(avg == 0 || avg == -1)
            return -1;                   // wrong result

        avg = avg / capacity;

        return avg;
    }
}

