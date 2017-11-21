import numpy as np
import pandas as pd
from matplotlib import pyplot as plt
from scipy.stats import pearsonr
from sklearn.model_selection import train_test_split
from sklearn.discriminant_analysis import LinearDiscriminantAnalysis as LDA, QuadraticDiscriminantAnalysis
from sklearn import metrics

def prepare_ds(name):
    ds = pd.read_csv(name)
    ds_attrs = ds.drop(['cov_type'], axis=1).values
    ds_class = ds['cov_type'].values
    return ds_attrs, ds_class

def main():
    attrs, classes = prepare_ds('cov_data.csv')

    for cls, color in zip(range(1,4), ('red', 'green', 'blue')):
        attr_one = attrs[:, 0][classes == cls]
        attr_two = attrs[:, 1][classes == cls]
        p = pearsonr(attr_one, attr_two)
        plt.scatter(x=attr_one, y=attr_two, marker='o', color=color,
                    label='cls: {:}, pearsonr={:.2f}'.format(cls, p[0]))

    plt.title('Pearson correlation')
    plt.xlabel('Elevation, m')
    plt.ylabel('Slope, num')
    plt.legend(loc='upper right')
    plt.show()

    data_train, data_test, class_train, class_test = train_test_split(
        attrs,
        classes,
        test_size=.3,
        random_state=123,
    )

    lda = LDA(n_components=2)
    lda_transform = lda.fit_transform(data_train, class_train)

    plt.figure(figsize=(10, 8))
    for cls, color in zip(range(1,4), ('red', 'green', 'blue')):
        attr_one = lda_transform[:, 0][class_train == cls]
        attr_two = lda_transform[:, 1][class_train == cls]
        plt.scatter(x=attr_one, y=attr_two, marker='o', color=color,
                    label='cls: {:}'.format(cls, p[0]))

    plt.xlabel('vec 1')
    plt.ylabel('vec 2')
    plt.legend()
    plt.show()

    lda_clf = LDA()
    lda_clf.fit(data_train, class_train)

    pred_train_lda = lda_clf.predict(data_train)
    print('Точность классификации на обучающем наборе данных (LDA): {:.2%}'.format(
        metrics.accuracy_score(class_train, pred_train_lda)))

    pred_test_lda = lda_clf.predict(data_test)
    print('Точность классификации на тестовом наборе данных (LDA): {:.2%}'.format(
        metrics.accuracy_score(class_test, pred_test_lda)))

    qda_clf = QuadraticDiscriminantAnalysis()
    qda_clf.fit(data_train, class_train)

    pred_train_qda = qda_clf.predict(data_train)
    print('Точность классификации на обучающем наборе данных (QDA): {:.2%}'.format(
        metrics.accuracy_score(class_train, pred_train_qda)))

    pred_test_qda = qda_clf.predict(data_test)
    print('Точность классификации на тестовом наборе данных (QDA): {:.2%}'.format(
        metrics.accuracy_score(class_test, pred_test_qda)
    ))

if __name__ == '__main__':
    main()
