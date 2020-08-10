using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace EA_Neo4j_SafetyAnalysis_AddIn
{
    internal class SetQueue
    {
        private HashSet<string> set;

        private Queue<string> queue;

        internal SetQueue()
        {
            set = new HashSet<string>();
            queue = new Queue<string>();
        }

        internal void clear()
        {
            set.Clear();
            queue.Clear();
        }

        internal bool isEmpty()
        {
            return queue.Count == 0;
        }

        internal string dequeue()
        {
            return queue.Dequeue();
        }

        internal string peek()
        {
            return queue.Peek();
        }

        internal void enqueue(string elementString)
        {
            if (set.Add(elementString))
            {
                queue.Enqueue(elementString);
            }
        }
    }
}
